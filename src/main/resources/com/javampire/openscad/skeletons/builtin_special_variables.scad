// OpenSCAD special variables (read-only at runtime).
// Declarations here exist for IDE resolve, completion, and documentation only.

// Minimum angle (in degrees) of each fragment.
$fa = 12;

// Minimum circumferential length of each fragment.
$fs = 2;

// Maximum discretization error for curved primitives.
$fe = undef;

// Number of fragments for curved primitives. Values of 3 or more override $fa and $fs.
$fn = 0;

// Animation time step in the range [0:1] when animation is enabled.
$t = 0;

// Viewport rotation angles in degrees.
$vpr = [0, 0, 0];

// Viewport translation.
$vpt = [0, 0, 0];

// Viewport camera distance.
$vpd = 100;

// Viewport camera field of view.
$vpf = 22.758;

// Number of module children in the current scope.
$children = 0;

// True during F5 preview, false during F6 render (since OpenSCAD 2019.05).
// From the command line: true only when exporting PNG with OpenCSG; false for STL and other formats.
$preview = false;
