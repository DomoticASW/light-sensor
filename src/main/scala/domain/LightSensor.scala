package domain

trait LightSensor

object LightSensor:
  enum State:
    case Light
    case DimLight
    case Dark

  enum Event:
    case Light_To_DimLight
    case DimLight_To_Dark
    case Dark_To_DimLight
    case DimLight_To_Light

