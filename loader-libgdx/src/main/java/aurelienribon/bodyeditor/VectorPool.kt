package aurelienribon.bodyeditor

import com.badlogic.gdx.math.Vector2

class VectorPool
{
    private val pool:MutableList<Vector2> = ArrayList()

    fun newVec():Vector2
    {
        return if (pool.isEmpty()) Vector2() else pool.removeAt(pool.size-1)
    }

    fun free(vec:Vector2)
    {
        pool.add(vec)
    }
}
