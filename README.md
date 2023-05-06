
## [Wave function collapse](https://en.wikipedia.org/wiki/Wave_function_collapse#Use_in_procedural_generation)

**Java implementation of the WFC-algorithm**


### What does it do:

In short, it takes in a 2D array of integer values (Could be colors, tiles or whatever you want),
goes over every possible 3x3 patterns of values represented in the input array and stores the possibilities
in a collection of unique patterns.

Then from that collection, outputs a new 2D array of integers that adheres to the same rules of patterns. 
That's it. A relatively simple input can generate complex outputs. There are many use cases for WFC in procedural generation.

### How does it work

I am not going to go over the details, as there are many excellent sources that explains it better
than I could. And some of my [favorites are linked below](#sources)

### How this might differ from other implementations

(Read when you have a fair understanding, also the code is commented)

A "Cells" remaining options are stored as bits (BitSet) instead of objects or integers. Propagation
and finding valid adjacent patterns etc. is done through simple logical operators. (and/or)
You don't have to look through a list and remove items or anything like that.
I.e. If a cell propagates to the next, simply Cell_A && Cell_B.

The priority queue used to sort Cells by entropy (Which to collapse next) is specialized on updating the priority
of its items.

Propagation is done "depth first" (Stack, not a Queue) but adjacent target cells are sorted by entropy
before they are pushed onto the propagation stack.

Cells do not propagate back in the direction they were propagated from.

A small amount of noise is introduced to cells' entropy (possibly better rng)


![cave system](https://github.com/heathensoft/WaveFunctionCollapse/blob/main/gif/wfc_caves.gif?raw=true)
![knots](https://github.com/heathensoft/WaveFunctionCollapse/blob/main/gif/wfc_knots.gif?raw=true)
![rooms](https://github.com/heathensoft/WaveFunctionCollapse/blob/main/gif/wfc_rooms.gif?raw=true)
![flowers](https://github.com/heathensoft/WaveFunctionCollapse/blob/main/gif/wfc_flowers.gif?raw=true)

# Sources
