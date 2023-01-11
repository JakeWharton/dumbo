package com.jakewharton.dumbo

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MastodonApiTest {
	@Test fun namedEntityUnescaping() {
		val input = StatusEntity(
			id = "1",
			rawContent = """Hacked together an ActionBar helper which uses native on 3.0+ and GreenDroid on pre-3.0 through single API. Will polish &amp; release this week."""
		)
		val expected = "Hacked together an ActionBar helper which uses native on 3.0+ and GreenDroid on pre-3.0 through single API. Will polish & release this week."
		assertThat(input.content).isEqualTo(expected)
	}

	@Test fun multipleParagraphsAndLinks() {
		val input = StatusEntity(
			id = "1",
			rawContent = """<p>ThreeTenABP 1.4.3 released which bumps the ThreeTenBP dependency to 1.6.4 and includes the 2022f tzdb.</p><p>ThreeTenABP changes: <a href="https://github.com/JakeWharton/ThreeTenABP/blob/trunk/CHANGELOG.md#version-143-2022-11-03" target="_blank" rel="nofollow noopener noreferrer"><span class="invisible">https://</span><span class="ellipsis">github.com/JakeWharton/ThreeTe</span><span class="invisible">nABP/blob/trunk/CHANGELOG.md#version-143-2022-11-03</span></a></p><p>ThreeTenBP changes: <a href="https://www.threeten.org/threetenbp/changes-report.html#a1.6.4" target="_blank" rel="nofollow noopener noreferrer"><span class="invisible">https://www.</span><span class="ellipsis">threeten.org/threetenbp/change</span><span class="invisible">s-report.html#a1.6.4</span></a></p><p>2022f tzdb changes: <a href="https://mm.icann.org/pipermail/tz-announce/2022-October/000075.html" target="_blank" rel="nofollow noopener noreferrer"><span class="invisible">https://</span><span class="ellipsis">mm.icann.org/pipermail/tz-anno</span><span class="invisible">unce/2022-October/000075.html</span></a></p>"""
		)
		val expected = """
			|ThreeTenABP 1.4.3 released which bumps the ThreeTenBP dependency to 1.6.4 and includes the 2022f tzdb.
			|
			|ThreeTenABP changes: https://github.com/JakeWharton/ThreeTenABP/blob/trunk/CHANGELOG.md#version-143-2022-11-03
			|
			|ThreeTenBP changes: https://www.threeten.org/threetenbp/changes-report.html#a1.6.4
			|
			|2022f tzdb changes: https://mm.icann.org/pipermail/tz-announce/2022-October/000075.html
			""".trimMargin()
		assertThat(input.content).isEqualTo(expected)
	}
}
