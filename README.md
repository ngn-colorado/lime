LIME
====
	Implementing LIME as a proxy server between Openflow controller and switches 
	to migrate live networks and VMs. LIME should be considered to be in a 
	"pre-alpha" state. There still are numerous bugs and missing features,
	and little to no documentation or necessary error-checking and validation.
	It can be used for testing, but please be aware that it is still in active
	development.
	
	The Javadoc for Lime, and by extension, the forked version of FLowvisor that
	we use is available at [https://ngn-colorado.github.io/lime](https://ngn-colorado.github.io/lime)

FlowVisor
=========
    An OpenFlow controller that acts as a hypervisor/proxy
    between a switch and multiple controllers.  Can slice
    multiple switches in parallel, effectively slicing a network.

Documentation
=============

    Start with the INSTALL file and then refer to:
        https://www.flowvisor.org
    Also, the manpages are in the ./doc directory and can be viewed
    without installing them using `man ./doc/fvctl.1`

    For developers, check out README.dev and the architecture diagrams:
        https://github.com/OPENNETWORKINGLAB/flowvisor/wiki under the developement section
    Also, `make docs` produces the source code documentation and
    there are manpages under ./doc

Questions
=========

    openflow-discuss@openflowswitch.org



