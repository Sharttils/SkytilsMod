/*
 * Sharttils - Hypixel Skyblock Quality of Life Mod
 * Copyright (C) 2022 Sharttils
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package sharttils.sharttilsmod.features.impl.handlers

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import sharttils.sharttilsmod.Sharttils
import sharttils.sharttilsmod.core.PersistentSave
import sharttils.sharttilsmod.utils.Utils
import sharttils.sharttilsmod.utils.graphics.colors.CustomColor
import java.io.*

class GlintCustomizer : PersistentSave(File(Sharttils.modDir, "customizedglints.json")) {

    override fun read(reader: InputStreamReader) {
        overrides.clear()
        glintColors.clear()
        for ((key, value) in gson.fromJson(reader, JsonObject::class.java).entrySet()) {
            val entry = value.asJsonObject
            if (entry.has("override")) {
                overrides[key] = entry["override"].asBoolean
            }
            if (entry.has("color")) {
                val color = Utils.customColorFromString(entry["color"].asString)
                glintColors[key] = color
            }
        }
    }

    override fun write(writer: OutputStreamWriter) {
        val obj = JsonObject()
        for ((key, value) in overrides) {
            val child = JsonObject()
            child.add("override", JsonPrimitive(value))
            obj.add(key, child)
        }
        for ((key, value) in glintColors) {
            val stringValue = value.toString()
            if (obj.has(key)) {
                obj[key].asJsonObject.addProperty("color", stringValue)
                continue
            }
            val child = JsonObject()
            child.add("color", JsonPrimitive(stringValue))
            obj.add(key, child)
        }
        gson.toJson(obj, writer)
    }

    override fun setDefault(writer: OutputStreamWriter) {
        gson.toJson(JsonObject(), writer)
    }

    companion object {

        @JvmField
        val overrides = HashMap<String, Boolean>()

        @JvmField
        val glintColors = HashMap<String, CustomColor>()
    }
}