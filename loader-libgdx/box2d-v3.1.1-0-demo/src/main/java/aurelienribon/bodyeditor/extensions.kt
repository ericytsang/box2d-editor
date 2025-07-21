package aurelienribon.bodyeditor

import com.badlogic.gdx.box2d.structs.b2Rot
import kotlin.math.atan2


fun b2Rot.radians():Float = atan2(s(),c())
