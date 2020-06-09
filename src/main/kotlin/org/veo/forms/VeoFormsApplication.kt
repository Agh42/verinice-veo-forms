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
package org.veo.forms

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.security.OAuthFlow
import io.swagger.v3.oas.annotations.security.OAuthFlows
import io.swagger.v3.oas.annotations.security.OAuthScope
import io.swagger.v3.oas.annotations.security.SecurityScheme
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
@SecurityScheme(name = VeoFormsApplication.SECURITY_SCHEME_OAUTH, type = SecuritySchemeType.OAUTH2,
        `in` = SecuritySchemeIn.HEADER,
        description = "openidconnect Login", flows = OAuthFlows(implicit = OAuthFlow(
        authorizationUrl = "\${spring.security.oauth2.resourceserver.jwt.issuer-uri}/protocol/openid-connect/auth",
        scopes = [OAuthScope(name = "veo-datenschutz",
                description = "Optional scope for access to specific content.")])))
class VeoFormsApplication {
    companion object {
        const val SECURITY_SCHEME_OAUTH = "OAuth2"
    }
}

fun main(args: Array<String>) {
    runApplication<VeoFormsApplication>(*args)
}