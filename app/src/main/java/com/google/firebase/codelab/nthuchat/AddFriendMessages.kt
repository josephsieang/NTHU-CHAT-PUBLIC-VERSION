package com.google.firebase.codelab.nthuchat

class AddFriendMessages {
    var addedTime: String? = null
    var name: String? = null

    constructor()

    constructor(addedTime: String, name: String) {
        this.addedTime = addedTime
        this.name = name
    }

}