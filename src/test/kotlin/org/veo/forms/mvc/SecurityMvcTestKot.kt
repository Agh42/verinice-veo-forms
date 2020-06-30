/**
 * Copyright (c) 2020 Jonas Jordan.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */
package org.veo.forms.mvc

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.springframework.http.HttpMethod

class SecurityMvcTestKot : AbstractMvcTest() {
    @TestFactory
    fun `regular API calls are forbidden without authorization`() = listOf(
            testStatus(HttpMethod.GET, "/", 401),
            testStatus(HttpMethod.POST, "/", 401),
            testStatus(HttpMethod.GET, "/a", 401),
            testStatus(HttpMethod.PUT, "/a", 401),
            testStatus(HttpMethod.DELETE, "/a", 401)
    )

    @TestFactory
    fun `documentation is accessible`() = listOf(
            testStatus(HttpMethod.GET, "/health", 200),
            testStatus(HttpMethod.GET, "/swagger-ui.html", 302),
            testStatus(HttpMethod.GET, "/swagger-ui/index.html", 200),
            testStatus(HttpMethod.GET, "/v3/api-docs", 200)
    )

    private fun testStatus(method: HttpMethod, url: String, status: Int): DynamicTest {
        return DynamicTest.dynamicTest("$method $url results in $status") {
            request(method, url).response.status shouldBe status
        }
    }
}
