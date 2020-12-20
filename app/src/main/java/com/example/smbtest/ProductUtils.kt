@file:JvmName("ProductUtils")
@file:JvmMultifileClass

fun checkData(name: String, price: Float?): Boolean {
    if (name.isNotEmpty() && price != null && price >= 0.0f)
        return true
    return false
}