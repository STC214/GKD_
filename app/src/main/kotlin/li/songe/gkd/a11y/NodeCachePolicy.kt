package li.songe.gkd.a11y

data class NodeCachePolicy(
    val textNodeMillis: Long,
    val structuralNodeMillis: Long,
) {
    init {
        require(textNodeMillis in 100L..2_000L)
        require(structuralNodeMillis in textNodeMillis..2_000L)
    }

    fun expiryMillis(hasText: Boolean): Long {
        return if (hasText) textNodeMillis else structuralNodeMillis
    }

    companion object {
        val Default = NodeCachePolicy(
            textNodeMillis = 500L,
            structuralNodeMillis = 1_000L,
        )

        /** Conservative fallback for devices where frequent Binder reads are unstable. */
        val Legacy = NodeCachePolicy(
            textNodeMillis = 1_000L,
            structuralNodeMillis = 2_000L,
        )
    }
}
