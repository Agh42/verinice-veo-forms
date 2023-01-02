/**
 * verinice.veo forms
 * Copyright (C) 2021 Daniel Murygin
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.veo.forms.migrations

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

class V2__addColumnSorting : BaseJavaMigration() {
    override fun migrate(context: Context) {
        context.connection.createStatement().use {
            it.execute(
                """
                alter table if exists form 
                    add column sorting varchar(32);
                """,
            )
        }
    }
}
