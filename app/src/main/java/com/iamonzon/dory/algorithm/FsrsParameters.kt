package com.iamonzon.dory.algorithm

/**
 * FSRS-4.5 algorithm parameters.
 *
 * Contains the 17 weights (w0-w16) and the desired retention target.
 * Default values are from the published FSRS-4.5 defaults, optimized
 * across hundreds of millions of reviews.
 */
data class FsrsParameters(
    val w: DoubleArray = DEFAULT_WEIGHTS,
    val desiredRetention: Double = DEFAULT_DESIRED_RETENTION
) {
    init {
        require(w.size == 17) { "FSRS-4.5 requires exactly 17 parameters, got ${w.size}" }
        require(desiredRetention in 0.70..0.97) {
            "Desired retention must be between 0.70 and 0.97, got $desiredRetention"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FsrsParameters) return false
        return w.contentEquals(other.w) && desiredRetention == other.desiredRetention
    }

    override fun hashCode(): Int {
        var result = w.contentHashCode()
        result = 31 * result + desiredRetention.hashCode()
        return result
    }

    companion object {
        const val DEFAULT_DESIRED_RETENTION = 0.9

        val DEFAULT_WEIGHTS = doubleArrayOf(
            0.4872,  // w0:  Initial stability for Again
            1.4003,  // w1:  Initial stability for Hard
            3.7145,  // w2:  Initial stability for Good
            13.8206, // w3:  Initial stability for Easy
            5.1618,  // w4:  Initial difficulty base
            1.2298,  // w5:  Initial difficulty scaling
            0.8975,  // w6:  Difficulty update rate
            0.031,   // w7:  Mean reversion weight
            1.6474,  // w8:  Stability increase base
            0.1367,  // w9:  Stability-dependent factor
            1.0461,  // w10: Retrievability-dependent factor
            2.1072,  // w11: Post-lapse stability base
            0.0793,  // w12: Difficulty factor for lapse
            0.3246,  // w13: Stability factor for lapse
            1.587,   // w14: Retrievability factor for lapse
            0.2272,  // w15: Hard penalty
            2.8755   // w16: Easy bonus
        )

        val DEFAULT = FsrsParameters()
    }
}
