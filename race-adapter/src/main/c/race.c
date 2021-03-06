/*
 * Copyright (c) 2017, United States Government, as represented by the
 * Administrator of the National Aeronautics and Space Administration.
 * All rights reserved.
 *
 * The RACE - Runtime for Airspace Concept Evaluation platform is licensed
 * under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * this file contains the toplevel raceserver/client functions called from application
 * specific code
 */

#include <errno.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <time.h>
#include <unistd.h>
#include <pthread.h>

#include "race_internal.h"

//--- internal functions

static local_endpoint_t *initialize_local(local_context_t *context) {
    const char *err_msg;

    int fd = race_server_socket(context->port, &err_msg);
    if (fd < 0) {
        context->error("failed to open socket (%s)\n", err_msg);
        return NULL;
    }

    local_endpoint_t *local = malloc(sizeof(local_endpoint_t));
    local->fd = fd;
    local->is_non_blocking = false;
    local->db = race_create_databuf(MAX_MSG_LEN);

    return local;
}

/*
 * assemble and send a DATA message
 */
static bool send_data(local_context_t *context, local_endpoint_t *local,
                      remote_endpoint_t *remote) {
    const char *err_msg;
    databuf_t *db = local->db;

    int pos = race_begin_write_data(db, SERVER_ID);
    pos = context->write_data(db,pos);  // acquire data via app specific callback
    if (pos >= 0) {
        pos = race_end_write_data(db,pos);
        if (pos > 0) {
            if (sendto(local->fd, db->buf, pos, 0, remote->addr, remote->addrlen) < 0) {
                context->error("sending track data failed (%s)", strerror(errno));
                return false;
            }
        }
    } else {
        context->warning("no data payload written");  // ? should this be a warning ?
    }

    return true;
}

/*
 * send a STOP message
 */
static void send_stop(local_context_t *context, local_endpoint_t *local,
                      remote_endpoint_t *remote) {
    const char *err_msg;
    databuf_t *db = local->db;

    race_write_stop(db, local->id);
    if (sendto(local->fd, db->buf, db->pos, 0, remote->addr, remote->addrlen) < 0) {
        context->error("sending local stop failed (%s)", strerror(errno));
    }
}

static void local_terminated(local_context_t *context, local_endpoint_t *local) {
    context->info("local terminating\n");

    if (close(local->fd) < 0) {
        context->error("closing socket failed (%s)", strerror(errno));
    }
}

static int n_remote = 0; // number of remote clients we are connected to

/*
 * block until we get a REQEUST message, then use context callbacks to determine if
 * we send a ACCEPT or REJECT message in response
 * 
 * TODO - this should handle schema versions
 */
static remote_endpoint_t* wait_for_request (local_context_t* context, local_endpoint_t* local) {
    const char* err_msg;
    databuf_t* db = local->db;

    // make sure we are in blocking mode before we wait for a remote
    local->is_non_blocking = !race_set_blocking(local->fd, &err_msg);
    if (local->is_non_blocking){
        context->error("cannot put socket into blocking mode (%s)\n", err_msg);
        return NULL;
    }

    socklen_t addrlen;
    struct sockaddr* src_addr = race_create_sockaddr(&addrlen);

    context->info("waiting for request..\n");
    int nread = recvfrom( local->fd, db->buf, db->capacity, 0, src_addr, &addrlen);
    if (nread > 0) {
        db->pos = nread;
        
        int req_flags = 0;
        int req_interval_msec = 0;
        epoch_msec_t sim_msec = 0;
        epoch_msec_t time_sent = 0;
        char req_schema[MAX_SCHEMA_LEN];

        if (!race_read_request(local->db, &time_sent, &req_flags, req_schema, MAX_SCHEMA_LEN, &sim_msec, &req_interval_msec, &err_msg)){
            context->error("error reading remote request (%s)\n", err_msg);
            free(src_addr);
            return NULL;
        }

        // who are we talking to
        char client_host[NI_MAXHOST];
        char client_service[NI_MAXSERV];
        getnameinfo(src_addr,addrlen, client_host,sizeof(client_host), client_service,sizeof(client_service),NI_NUMERICHOST);
        
        // check possible reasons for rejection
        int reject = context->check_request(client_host, client_service, req_flags, req_schema,
                                            sim_msec, &req_interval_msec);
        if (reject) {
            context->info("remote rejected for reason %x\n", reject);
            race_write_reject(db, reject);
            if (sendto(local->fd, db->buf, db->pos, 0, src_addr, addrlen) < 0) {
                context->error("sending local response failed (%s)", strerror(errno));
            }
            free(src_addr);
            return NULL;
        }

        // remote is accepted, set local state accordingly 
        local->interval_msec = req_interval_msec;

        // check if we have to apply a time difference
        long time_diff = race_epoch_msec() - sim_msec;  // TODO - this should acquire sim time from the context
        if (labs(time_diff) > MAX_TIME_DIFF) {
            context->info("adapting simulation time by %d sec\n", time_diff / 1000);
            context->time_diff = time_diff;
        }

        int remote_id = ++n_remote;
        race_write_accept(db, context->flags, local->interval_msec, remote_id);
        if (sendto(local->fd, db->buf, db->pos, 0, src_addr, addrlen) < 0) {
            context->error("sending local accept failed (%s)", strerror(errno));
            free(src_addr);
            return NULL;
        }

        remote_endpoint_t* remote = malloc(sizeof(remote_endpoint_t));
        remote->addr = src_addr;
        remote->addrlen = addrlen;
        remote->id = remote_id;
        remote->time_request = remote->time_last = time_sent;
        remote->is_stopped = false;
        return remote;

    } else {
        if (!context->stop_local) {
            context->error("reading remote request failed (%s)\n", strerror(errno));
        }
        return NULL;
    }
}

/*
 * wait blocking until we receive a message from the remote endpoint, process system messages
 * and pass data messages to the application specific context callback
 */
static void receive_message(local_context_t *context, local_endpoint_t *local, remote_endpoint_t *remote) {
    databuf_t db = DATABUF(MAX_MSG_LEN);
    const char *err_msg;
    int remote_id;
    epoch_msec_t send_time;

    int n_read = recvfrom(local->fd, db.buf, db.capacity, 0, remote->addr, &remote->addrlen);
    if (n_read > 0) {
        db.pos = n_read;

        if (race_is_stop(&db)) {
            if (race_read_stop(&db, &remote_id, NULL, &err_msg) && remote_id == remote->id) {
                remote->is_stopped = true;
            }

        } else if (race_is_data(&db)) {
            if (context->flags & DATA_RECEIVER) {
                int pos = race_read_data_header(&db, &remote_id, &send_time, &err_msg);
                if (pos && remote_id == remote->id && send_time >= remote->time_last) {
                    remote->time_last = send_time;
                    context->read_data(&db,pos);
                } else {
                    context->warning("ignoring out-of-order message from remote %x (%s)\n", remote_id, err_msg);
                }
            } else {
                context->warning("local is ignoring track messages\n");
            }
        } else {
            context->warning("received unknown message\n");
        }
    } else {
        context->error("polling remote failed (%s)\n", strerror(errno));
    }
}

/*
 * thread function that loops receive_message() calls until one of the end points is stopped
 */
static void* receive_messages_thread(void* args) {
    local_context_t *context = ((threadargs_t*)args)->context;
    local_endpoint_t *local = ((threadargs_t*)args)->local;
    remote_endpoint_t *remote = ((threadargs_t*)args)->remote;

    context->info("receiver thread started\n");

    while (!remote->is_stopped && !context->stop_local) {
        receive_message(context, local, remote);
        // TODO - shall we safeguard against starvation of other threads here?
    }

    context->info("receiver thread terminated\n");
    pthread_exit(NULL);
}

/*
 * poll remote messages (non-blocking)
 */
static void poll_messages (local_context_t* context, local_endpoint_t* local, remote_endpoint_t* remote) {
    const char* err_msg;
    int n_msgs = 0;
    
    local->is_non_blocking = race_set_nonblocking(local->fd, &err_msg); // so that we can poll remote messages
    if (!local->is_non_blocking && (context->flags & DATA_RECEIVER)) {
        context->warning("cannot receive data from remote, socket is blocking (%s)\n", err_msg);
    }

    if (local->is_non_blocking){
        while (race_check_available(local->fd, &err_msg) > 0 && n_msgs < 42) {
            receive_message(context, local, remote);
        }
    }
}

//--- exported functions (to be used by application specific code)

/*
 * send data at a fixed interval and poll the remote endpoint synchronously before each send
 */
bool race_interval_poll(local_context_t *context) {
    const char *err_msg;

    if (context == NULL) {
        perror("no local context");
        return false;
    }

    local_endpoint_t *local = initialize_local(context);
    if (local != NULL) {
        while (!context->stop_local) { // outer loop - remote connection
            remote_endpoint_t *remote = wait_for_request(context, local); 
            if (remote != NULL) {
                local->is_non_blocking = race_set_nonblocking(
                    local->fd, &err_msg); // so that we can poll remote messages
                if (!local->is_non_blocking && (context->flags & DATA_RECEIVER)) {
                    context->warning("cannot receive data from remote, socket is blocking (%s)\n",
                                     err_msg);
                }

                while (!remote->is_stopped && !context->stop_local) { // inner loop - remote data exchange
                    poll_messages(context, local, remote); // this might change remote state

                    if (!remote->is_stopped) {
                        if (!send_data(context, local, remote)) {
                            break;
                        }
                        race_sleep_msec(context->interval_msec);
                    }
                }

                if (context->stop_local && !remote->is_stopped) {
                    send_stop(context, local, remote);
                }
                free(remote);
            }
        }
    }

    local_terminated(context, local);
    free(local);
    return true;
}

/*
 * send data at a fixed interval and receive remote endpoint data asynchronously from a separate thread
 */
bool race_interval_threaded(local_context_t *context) {
    const char *err_msg;

    if (context == NULL) {
        perror("no local context");
        return false;
    }

    local_endpoint_t *local = initialize_local(context);
    if (local != NULL) {
        while (!context->stop_local) { // outer loop - remote connection
            remote_endpoint_t *remote = wait_for_request(context, local);
            if (remote != NULL) {
                pthread_t receiver;
                threadargs_t args = { context,local,remote };
                int rc = pthread_create( &receiver, NULL, receive_messages_thread, &args);
                if (rc) {
                    context->error("failed to create receiver thread (%s)\n", strerror(rc));
                    break;
                }

                while (!remote->is_stopped && !context->stop_local) {  // inner loop - remote data exchange
                    if (!remote->is_stopped) {
                        if (!send_data(context, local, remote)) {
                            break;
                        }
                        race_sleep_msec(local->interval_msec);
                    }
                }

                if (context->stop_local && !remote->is_stopped) {
                    send_stop(context, local, remote);
                }

                pthread_cancel(receiver);  // TODO - shall we use less harsh measures to terminate?
                pthread_join(receiver,NULL);
                free(remote);
            }
        }
    }

    local_terminated(context, local);
    free(local);
    return true;
}