package com.skyecodes.skyetools.downloader

import com.fasterxml.jackson.annotation.JsonProperty

class DownloadRequest {
    lateinit var url: String
    var type: Type = Type.ALL
    var quality: Quality = Quality.BEST
    var size: Size = Size.NO_LIMIT
    var preferFreeFormats: Boolean = false

    enum class Type(val str: String) {
        @JsonProperty("all")
        ALL(""),
        @JsonProperty("audio")
        AUDIO("a"),
        @JsonProperty("video")
        VIDEO("v")
    }

    enum class Quality(val str: String) {
        @JsonProperty("best")
        BEST("b"),
        @JsonProperty("worst")
        WORST("w")
    }

    enum class Size(val str: String? = null) {
        @JsonProperty("0")
        NO_LIMIT,
        @JsonProperty("25")
        LIMIT_25M("25M"),
        @JsonProperty("50")
        LIMIT_50M("50M"),
        @JsonProperty("500")
        LIMIT_500M("500M")
    }
}
