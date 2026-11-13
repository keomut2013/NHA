package com.iptv.player.data

data class Channel(
    val name: String,
    val streamUrl: String,
    val group: String = "Tổng Hợp",
    val logoUrl: String? = null,
    val userAgent: String? = null
)

object M3uParser {
    fun parse(content: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        val lines = content.lines()

        var currentName = ""
        var currentGroup = "Tổng Hợp"
        var currentLogo: String? = null
        var currentUserAgent: String? = null

        val groupRegex = Regex("""group-title="([^"]+)"""")
        val logoRegex = Regex("""tvg-logo="([^"]+)"""")
        val uaRegex = Regex("""http-user-agent=([^\r\n]+)""")

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#EXTINF:")) {
                // Extract group
                groupRegex.find(trimmed)?.let { currentGroup = it.groupValues[1] }
                // Extract logo
                logoRegex.find(trimmed)?.let { currentLogo = it.groupValues[1] }
                // Extract channel title after last comma
                val commaIndex = trimmed.lastIndexOf(',')
                if (commaIndex != -1 && commaIndex < trimmed.length - 1) {
                    currentName = trimmed.substring(commaIndex + 1).trim()
                } else {
                    currentName = "Kênh không tên"
                }
            } else if (trimmed.startsWith("#EXTVLCOPT:")) {
                uaRegex.find(trimmed)?.let { currentUserAgent = it.groupValues[1] }
            } else if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                // This is the stream URL
                if (currentName.isNotEmpty()) {
                    channels.add(
                        Channel(
                            name = currentName,
                            streamUrl = trimmed,
                            group = currentGroup,
                            logoUrl = currentLogo,
                            userAgent = currentUserAgent
                        )
                    )
                }
                // Reset metadata for next channel
                currentName = ""
                currentGroup = "Tổng Hợp"
                currentLogo = null
                currentUserAgent = null
            }
        }
        return channels
    }
}

