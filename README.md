## Inspiration

This was inspired by [SwipeStack](https://github.com/flschweiger/SwipeStack) and other libraries like it.

## Screenshots

![](screenshot.gif)

## Usage

```xml
<chi.widget.StackView
    android:id="@+id/stackView"
    android:layout_width="match_parent"
    android:layout_height="280dp"
    android:paddingBottom="32dp"
    android:paddingStart="16dp"
    android:paddingEnd="16dp"
    app:stack_max_size="3"
    app:stack_spacing="12dp"
    android:background="#8EB54E" />
```

## Download

Step 1. Add the JitPack repository to your root `build.gradle` at the end of **`allprojects`** `repositories`
```
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```
Step 2. Add the `StackView` dependency
```
dependencies {
    ...
    implementation 'com.github.MostafaGazar:StackView:1.0.0'
    ...
}
```

## Developed by

[Mostafa Gazar](https://www.linkedin.com/in/mostafagazar)
