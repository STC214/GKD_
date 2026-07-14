package li.songe.gkd.shizuku

/**
 * 聚合一次输入序列中 DOWN/MOVE/UP 的结果。
 * 任一步失败后必须保持失败，后续成功的 UP 不能覆盖此前失败。
 */
internal class InputSequenceResult {
    var succeeded: Boolean = true
        private set

    fun record(result: Boolean): Boolean {
        succeeded = result && succeeded
        return result
    }
}
