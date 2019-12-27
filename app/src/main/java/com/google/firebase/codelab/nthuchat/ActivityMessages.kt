package com.google.firebase.codelab.nthuchat

class ActivityMessages {

    var creator: String? = null
    var creatorName: String? = null
    var description: String? = null
    var enddate: String? = null
    var location: String? = null
    var startdate: String? = null
    var title: String? = null


    constructor()

    constructor(creator: String, creatorName: String, description: String, enddate: String, location: String, startdate: String, title: String) {
        this.creator = creator
        this.creatorName = creatorName
        this.description = description
        this.enddate = enddate
        this.location = location
        this.startdate = startdate
        this.title = title
    }

}