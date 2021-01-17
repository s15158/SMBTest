@file:JvmName("ProductUtils")
@file:JvmMultifileClass

fun checkData(name: String, description: String, radius: Int?): Boolean {
    if (name.isNotEmpty() && description.isNotEmpty() && radius != null && radius >= 0)
        return true
    return false
}