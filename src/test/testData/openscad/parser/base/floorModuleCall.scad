// Builtin names like floor() can be used as module calls when user-defined modules shadow them.
module bowl() {
    floor();
}

module floor() {
    children();
}
