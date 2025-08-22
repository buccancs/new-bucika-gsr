package com.multisensor.recording.simple

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class SimpleInfrastructureTest {

    @Test
    fun `simple test should pass`() {
        assertThat(1 + 1).isEqualTo(2)
    }

    @Test
    fun `string operations should work`() {
        val testString = "Hello World"
        assertThat(testString).contains("Hello")
        assertThat(testString).hasLength(11)
    }

    @Test
    fun `boolean logic should work`() {
        assertThat(true).isTrue()
        assertThat(false).isFalse()
    }
}
