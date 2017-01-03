package gov.nasa.race.uom


/**
  * area quantities
  * underlying unit is square meter
  */
object Area {
  final implicit val εArea = SquareMeters(1e-10)

  //--- constructors
  def SquareMeters (d: Double) = new Area(d)


  implicit class AreaConstructor (val d: Double) extends AnyVal {
    @inline def squareMeters = SquareMeters(d)
    @inline def `m²` = SquareMeters(d)
  }
}

/**
  * derived unit for squared lengths
  * basis is m²
  */
class Area protected[uom] (val d: Double) extends AnyVal {

  @inline def toSquareMeters: Double = d

  @inline def / (x: Double): Area = new Area(d/x)
  @inline def / (x: Length)(implicit r: LengthDisambiguator.type): Length = new Length(d/x.d)
  @inline def * (x: Double): Area = new Area(d * x)

  @inline def + (x: Area): Area = new Area(d + x.d)
  @inline def - (x: Area): Area = new Area(d - x.d)

  @inline def ≈ (x: Area)(implicit εArea: Area) = Math.abs(d - x.d) <= εArea.d
  @inline def ~= (x: Area)(implicit εArea: Area) = Math.abs(d - x.d) <= εArea.d
  @inline def within (x: Area, tolerance: Area) = Math.abs(d - x.d) <= tolerance.d

  @inline def < (x: Area) = d < x.d
  @inline def > (x: Area) = d > x.d
  @inline def =:= (x: Area) = d == x.d
  @inline def ≡ (x: Area) = d == x.d
  // we intentionally omit ==, <=, >=

  override def toString = show
  def show = s"${d}m²"
}