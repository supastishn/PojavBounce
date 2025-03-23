package net.ccbluex.liquidbounce.features.command.builder


/**
 * A tokenized command. For example `.friend add SenkJu`
 *
 * @param tokens in that example: [`.friend` - 0, `add` - 8, `SenkJu` - 12]
 */
data class LexedParameters(
    private val sourceText: String,
    private val tokens: List<Token>,
    private val currentOffset: Int = 0,
) {
    val text: String
        get() =  this.sourceText.substring(this.tokens[this.currentOffset].startIdx)
    val size: Int
        get() = this.tokens.size - currentOffset

    operator fun get(idx: Int): String {
        return this.tokens[idx + this.currentOffset].text
    }

    fun asTokens() = this.tokens.map { it.text }

    /**
     * Returns a new lexer result with all params before the [paramIdx] excluded.
     *
     * For example: `.friend add SenkJu` with offset 1 -> `add SenkJu`
     */
    fun withOffset(paramIdx: Int): LexedParameters {
        check(this.tokens.size > paramIdx) { "Expected at least $paramIdx tokens" }

        return this.copy(currentOffset = paramIdx)
    }

    companion object {
        /**
         * Tokenizes the [line].
         *
         * For example: `.friend add "Senk Ju"` -> [[`.friend`, `add`, `Senk Ju`]]
         */
        fun tokenizeCommand(line: String): LexedParameters {
            val output = ArrayList<Token>()
            val stringBuilder = StringBuilder()

            var escaped = false
            var quote = false

            var idx = 0
            var startIdx = idx

            for (c in line.toCharArray()) {
                idx++

                // Was this character escaped?
                if (escaped) {
                    stringBuilder.append(c)

                    escaped = false
                    continue
                }

                // Is the current char an escape char?
                if (c == '\\') {
                    escaped = true // Enable escape for the next character
                } else if (quote && c == '"') {
                    quote = false
                } else if (stringBuilder.isEmpty() && c == '"') {
                    quote = true
                } else if (c == ' ' && !quote) {
                    // Is the buffer not empty? Also ignore stuff like .friend   add SenkJu
                    if (stringBuilder.trim().isNotEmpty()) {
                        output.add(Token(text = stringBuilder.toString(), startIdx = startIdx))

                        // Reset string buffer
                        stringBuilder.setLength(0)

                        startIdx = idx
                    }
                } else {
                    stringBuilder.append(c)
                }
            }

            // Is there anything left in the buffer?
            if (stringBuilder.trim().isNotEmpty()) {
                // If a string was not closed, don't remove the quote
                // e.g. .friend add "SenkJu -> [.friend, add, "SenkJu]
                val text = if (quote) {
                    '"' + stringBuilder.toString()
                } else {
                   stringBuilder.toString()
                }

                output.add(Token(text, startIdx))
            }

            return LexedParameters(line, output)
        }
    }

    data class Token(val text: String, val startIdx: Int)
}

