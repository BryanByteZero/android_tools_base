<?xml version="1.0" encoding="utf-8"?>
<android.support.wearable.view.WatchViewStub
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/watch_view_stub"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:rectLayout="@layout/${rectLayout}"
    app:roundLayout="@layout/${roundLayout}"
    tools:context="${relativePackage}.${activityClass}"
    tools:deviceIds="wear">
</android.support.wearable.view.WatchViewStub>