/**
 * Copyright Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.firebase.codelab.nthuchat

class FriendlyMessage {

    private var id: String? = null
    private var text: String? = null
    private var name: String? = null
    private var photoUrl: String? = null
    private var uid: String? = null

    constructor()

    constructor(text: String, name: String, photoUrl: String?, uid: String) {
        this.text = text
        this.name = name
        if (photoUrl != null && photoUrl.contains("..")) {
            this.photoUrl = "https://nthuchat.com" + photoUrl.replace("..", "")
        } else {
            this.photoUrl = photoUrl
        }
        this.uid = uid
    }

    fun setId(id: String) { this.id = id }

    fun setText(text: String) {
        if (text.contains("\n")) {
            val tmp_text = text.replace("\n".toRegex(), " ")
            this.text = tmp_text.trim { it <= ' ' }
        } else {
            this.text = text.trim { it <= ' ' }
        }
    }

    fun getName(): String? { return name }

    fun setName(name: String) { this.name = name }

    fun getUid(): String? { return uid }

    fun setUid(uid: String) { this.uid = uid }

    fun getPhotoUrl(): String? {
        if (photoUrl != null && photoUrl!!.contains("..")) {
            photoUrl = "https://nthuchat.com" + photoUrl?.replace("..", "")
            return photoUrl
        } else {
            return photoUrl
        }
    }

    fun getText(): String? {
        if (text!!.contains("\n")) {
            val tmp_text: String? = text?.replace("\n".toRegex(), " ")
            return tmp_text.toString().trim { it <= ' ' }
        } else {
            return text.toString().trim { it <= ' ' }
        }
    }

    fun setPhotoUrl(photoUrl: String) { this.photoUrl = photoUrl }

}

