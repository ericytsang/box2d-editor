package aurelienribon.bodyeditor

import aurelienribon.bodyeditor.BodyEditorLoader.XYModel
import com.badlogic.gdx.box2d.Box2d
import com.badlogic.gdx.box2d.Constants.B2_MAX_POLYGON_VERTICES
import com.badlogic.gdx.box2d.structs.b2BodyId
import com.badlogic.gdx.box2d.structs.b2Circle
import com.badlogic.gdx.box2d.structs.b2Hull
import com.badlogic.gdx.box2d.structs.b2Polygon
import com.badlogic.gdx.box2d.structs.b2ShapeDef
import com.badlogic.gdx.box2d.structs.b2ShapeId
import com.badlogic.gdx.box2d.structs.b2Vec2
import com.badlogic.gdx.math.Vector2

/**
 * Attaches fixtures to your Box2D version 3.1.1-0 body.
 * You only need to give it a body and the corresponding fixture name, and it will attach these fixtures to your body.
 */
object Box2dV3_1_1_0XFixtureAttacher {
    /**
     * Creates and applies the fixtures defined in the editor. The name
     * parameter is used to retrieve the right fixture from the loaded file.
     * <br></br><br></br>
     *
     *
     * The body reference point (the red cross in the tool) is by default
     * located at the bottom left corner of the image. This reference point
     * will be put right over the BodyDef position point. Therefore, you should
     * place this reference point carefully to let you place your body in your
     * world easily with its BodyDef.position point. Note that to draw an image
     * at the position of your body, you will need to know this reference point
     * (see [.getOrigin].
     * <br></br><br></br>
     *
     *
     * Also, saved shapes are normalized. As shown in the tool, the width of
     * the image is considered to be always 1 meter. Thus, you need to provide
     * a scale factor so the polygons get resized according to your needs (not
     * every body is 1 meter large in your game, I guess).
     *
     * @param body  The Box2d body you want to attach the fixture to.
     * @param name  The name of the fixture you want to load.
     * @param fd    The fixture parameters to apply to the created body fixture.
     * @param scale The desired scale of the body. The default width is 1.
     */
    @JvmStatic
    fun attachFixture(
        loader: BodyEditorLoader,
        bodyId: b2BodyId,
        name: String,
        shapeDef: b2ShapeDef,
        scale: XYModel,
    ):List<b2ShapeId> = loader.accept(
        name = name,
        scale = scale,
        visitor = Visitor(
            bodyId = bodyId,
            shapeDef = shapeDef,
        ),
    )

    private class Visitor(
        private val bodyId: b2BodyId,
        private val shapeDef: b2ShapeDef,
    ) : BodyEditorLoader.ShapeVisitor<b2ShapeId> {
        override fun visitPolygon(vertices: List<Vector2>):b2ShapeId {
            val verts = vertices.map { b2Vec2().apply { x(it.x); y(it.y) } }
            val polygon = createHullPolygon(verts)
            return Box2d.b2CreatePolygonShape(bodyId, shapeDef.asPointer(), polygon.asPointer())
        }

        override fun visitCircle(center: Vector2, radius: Float):b2ShapeId {
            val circle = b2Circle().apply {
                center().x(center.x)
                center().y(center.y)
                radius(radius)
            }
            return Box2d.b2CreateCircleShape(bodyId, shapeDef.asPointer(), circle.asPointer())
        }
    }

    /**
     * see [Box2d.b2ComputeHull] & [b2Polygon].
     *
     * important points:
     * - must be a convex shape (interior of the polygon is to the left of each edge)
     * - must have at least 3 vertices
     * - must have at most [B2_MAX_POLYGON_VERTICES] (8) vertices
     */
    private fun createHullPolygon(hullVerticesList: List<b2Vec2>):b2Polygon {

        // assert hullVertices.size
        require(hullVerticesList.size in 3..B2_MAX_POLYGON_VERTICES)
        {
            "hullVertices.size must be between 3 and $B2_MAX_POLYGON_VERTICES, but was ${hullVerticesList.size}"
        }

        // create the polygon from the hull vertices
        val hullVertices = b2Vec2.b2Vec2Pointer(hullVerticesList.size, true)
        hullVerticesList.forEachIndexed { index, vec2 -> hullVertices.set(vec2, index) }
        val hull:b2Hull = Box2d.b2ComputeHull(hullVertices, hullVerticesList.size)
        return Box2d.b2MakePolygon(hull.asPointer(), 0f)
    }
}
