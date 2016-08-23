# Swipe Action View

[![Build Status](https://travis-ci.com/Tunous/SwipeActionView.svg?token=axaKPJmKXjhfPLy7VR2f&branch=master)](https://travis-ci.com/Tunous/SwipeActionView)
[![License](https://img.shields.io/github/license/mashape/apistatus.svg)](https://github.com/Tunous/SwipeActionView/blobl/master/LICENSE)

SwipeActionViews is a swipe-able view, which allows users to perform various actions by swiping it to left or right side.

# Table of contents
- [Quick Example](#quick-example)
- [Sample](#sample)
- [Details](#details)
  - [Container](#container)
  - [Background views](#background-views)
- [Disabling gestures](#disabling-gestures)
- [Ripple animations](#ripple-animations)
- [Gesture listener](#gesture-listener)
- [Tools attributes](#tools-attributes)
- [Always drawing background attribute](#always-drawing-background-attribute)
- [Click listeners](#click-listeners)
- [Animations](#animations)
- [Credits](#credits)

TODO: Images here

# <a id="quick-example">Quick example</a>

Adding SwipeActionView to your library is as easy as adding it in xml and setting up swipe gesture listener.
Below example will create swipe-able TextView with 2 icon backgrounds that allows for swiping to the left and right side.

```xml
<me.thanel.swipeactionview.SwipeActionView
    android:id="@+id/swipe_view"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="?colorPrimary">

    <ImageView
        android:layout_width="24dp"
        android:layout_height="24dp"
        android:src="@mipmap/ic_launcher"/>

    <ImageView
        android:layout_width="24dp"
        android:layout_height="24dp"
        android:layout_gravity="end"
        android:src="@mipmap/ic_launcher"/>
        
    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:padding="16dp"
        android:text="Swipe me"/>
</me.thanel.swipeactionview.SwipeActionView>
```

```java
SwipeActionView swipeView = (SwipeActionView) findViewById(R.id.swipe_view);
swipeView.setSwipeGestureListener(new SwipeGestureListener() {
    @Override
    public boolean onSwipedLeft(@NotNull SwipeActionView swipeActionView) {
        showToast("Swiped left");
        return true;
    }

    @Override
    public boolean onSwipedRight(@NotNull SwipeActionView swipeActionView) {
        showToast("SwipedRight");
        return true;
    }
});
```

# <a id="sample">Sample</a>
For example implementation of SwipeActionView see the included sample. It contains both code and comment descriptions to make it easy to understand.

You can manually compile the apk or download from the releases page of this repository.

# <a id="details">Details</a>
To add a view which can be swiped simply crate a SwipeActionView in xml and add 2 or 3 views as its children.

```xml
<me.thanel.swipeactionview.SwipeActionView>
</me.thanel.swipeactionview.SwipeActionView>
```

The last child of SwipeActionView becomes a container and the rest of children become swipe views for directions corresponding to their layout gravity.

### <a id="container">Container</a>
The container is a view which is drawn above other views and it is what is actually swiped away.

### <a id="background-views">Background views</a>
The other children are views which are drawn behind container. You can specify for which swipe direction each of them corresponds by setting their layout_gravity attribute. View without it changed will appear on left side which means that it will be visible when user performs swipe right gesture. Setting it to end(right) will make the view be drawn on right side and correspond to swipe left gesture.

Note that there can be only view for each direction. This means that if one of them is placed on left side the second one must be placed on right side to avoid errors.

This behavior allows you to easily add single background and by specifying it's layout gravity determine whether SAV should be possible to be swiped only to the left or right side.

# <a id="disabling-gestures">Disabling gestures</a>
If you want to dynamically enable or disable gesture in specific direction you can use x method.

The gesture enabling is actually controlled by presence and visibility of background views and this method is only provided for convenience. You could easily just manually set these views to invisible or visible.

By having this behavior it's possible to have specific gesture disabled by default by simply having the visibility of view set to invisible or gone.

# <a id="ripple-animations">Ripple animations</a>
SAV comes with support of displaying ripple animations when gestures are performed. All you have to do to enable them is simply setting their color from xml. It's also possible to change it dynamically from code with x method. Setting color to -1 disables selected ripple animation.

# <a id="gesture-listener">Gesture listener</a>
In order to be able to perform actions when user swipes the view you have to set the listener with x method. It takes sgl as parameter.

Sgl has two methods. One for performing action when view is swiped to the left used and one when it is swiped to the right side.

Each of these methods returns boolean as a result. Most of the time you'll want to return true here. Returning false is designed for advanced usage. By doing so the view won't be automatically animated to original position but will stay at the full translation. This allows you to manipulate content of the visible background view. One great example of this is displaying progress wheel and manually returning view to original position once some long action finishes execution.

To return view to it's original position you can call x method at any time. Note: it's better to not call this while user is swiping as that could confuse them.

# <a id="tools-attributes">Tools attributes</a>
Similar to the idea of tools namespace in android sav has special attributes used only in editor mode. They make it possible to preview ripples or contents of background views without worrying about side effects. They are entirely ignored when running in device.

# <a id="always-drawing-background-attribute">Always drawing background attribute</a>
In order to reduce overdraw sav draws only the parts of background and background views which become visible due to swipe gesture. This is not always what you want as it would break any container views with transparency. Good example where you would want to use this attribute is when you had CardView as your container.

# <a id="click-listeners">Click listeners</a>
Sav makes sure that any click listeners will work correctly. This means that you can use setclicklistener as usual and they should work. This includes views located in container.

Only exception is that you shouldn't add click listeners for background views. This library wasn't designed to add support for this behavior. If it's possible then that's only a positive side effect. You are better of with using libraries such as ... instead.

# <a id="animations">Animations</a>
SwipeActionView comes with support for custom animations. There are 2 listeners that you can set in your code. They will be called with current swipe progress while user performs swipe gesture.

By default there is only one animator included which scales the background views. You can use it as an example on how to implement custom animations or use it directly if it's good enough for you.

# <a id="credits">Credits</a>
This application wouldn't happen without help and work of [Brian Robles].

His [KarmaMachine] Reddit application was direct inspiration for this library.
He also originally created SwipeRipleDrawable and allowed me to reimplement it for purposes of this library.
Huge thanks!

[Brian Robles]: https://github.com/brianrobles204
[KarmaMachine]: https://play.google.com/store/apps/details?id=com.brianrobles204.karmamachine
