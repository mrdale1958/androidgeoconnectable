

const minScaler = 0.1;

const machine = {
  dispatch(actionName, ...payload) {
    const actions = this.transitions[this.state];
    const action = this.transitions[this.state][actionName];

    if (action) {
      render(`action dispatched: ${ actionName }`);
      action.apply(machine, payload);
    }
  },
  changeStateTo(newState) {
    render(`state changed: ${ newState }`);
    this.state = newState;
  },
  state: 'idle',
  transitions: {
    'idle': {
      click: function () {
        this.changeStateTo('zoomingIn');
        this.dispatch('zoom_in');

      }
    },
    'zoomingIn': {
        fully_zoomed_in: function(){
            this.changeStateTo('open');
        },
        zoom_in: function(){
            atMax = increaseZoomOnMainDiv();
            if (atMax) {
                this.dispatch('fully_zoomed_in');
            }
        },
        zoom_out: function(){
             this.changeStateTo('zoomingOut');
             this.dispatch('zoom_out');
       }
     },
    'zoomingOut': {
        fully_zoomed_out: function(){
        this.changeStateTo('idle');
        },
        zoom_in: function(){
            this.changeStateTo('zoomingIn');
            this.dispatch('zoom_in');
        },
        zoom_out: function(){
            atMin = decreaseZoomOnMainDiv();
            if (atMin) {
                this.dispatch('fully_zoomed_out');
            }
        }
     },
    'open': {
        click_in: function(){
        },
        click_out: function(){
            this.changeStateTo('zooming_out');
            this.dispatch('zoom_out');
        },
        click_left: function(){
        },
        click_right: function(){
        },
        click_up: function(){
        },
        click_down: function(){
        }
     },
    'slide_show': {
        zoom_in: function(){
        },
        zoom_out: function(){
        this.changeStateTo('zooming_out');
        this.dispatch('zoom_out');
        },
     },
     'paging': {
      click_left: function(){
         },
         click_right: function(){
         },
         click_up: function(){
         },
         click_down: function(){
         },
        slide_left: function(){
        },
        slide_right: function(){
        },
        slide_up: function(){
        },
        slide_down: function(){
        },
     },

     'error': {
        retry: function () {
            this.changeStateTo('idle');
            this.dispatch('click');
          }
     }
  }
}

function increaseZoomOnMainDiv() {
    var currentTransform = mainDiv.style.transform;
    var scaler = 0.0;

    if (currentTransform.indexOf('scale(' == 0))
    {
        scaler = currentTransform.substring(currentTransform.indexOf('(') +1, currentTransform.indexOf(')'));
        if (scaler.indexOf(',') > 0) scaler = scaler.substring(0,scaler.indexOf(','));
        scaler = Math.min(1.0, Number(scaler) + zoomIncrement);
    } else {
        scaler = minScaler;
    }
    currentTransform = 'scale(' + scaler +');';
    mainDiv.style.transform = currentTransform;
    return scaler;
}

function decreaseZoomOnMainDiv() {
    var currentTransform = mainDiv.style.transform;
    var scaler = 0.0;
    if (currentTransform.indexOf('scale(' == 0))
    {
        scaler = currentTransform.substring(currentTransform.indexOf('(') +1, currentTransform.indexOf(')'));
        if (scaler.indexOf(',') > 0) scaler = scaler.substring(0,scaler.indexOf(','));
        scaler = Math.max(0.0, Number(scaler) - zoomIncrement);
        currentTransform = 'scale(' + scaler +');'
    } else {
              scaler = 1.0;
    }
    currentTransform = 'scale(' + scaler +');';
    mainDiv.style.transform = currentTransform;
    return scaler;

}

var TableInterface = {
    constructor: function(mainDiv,transitionType,...internals) {
		this.tiltVector = [0.0, 0.0];
		this.zoom = 0.0;
		this.mainDiv = mainDiv;
		this.fsm = machine;
	},
	update: function update(tiltVector, zoom) {
	    this.tiltVector = [this.tiltVector[0] + tiltVector[0], this.tiltVector[1] + tiltVector[1]];
	    if (Math.abs(this.tiltVector[0]) > tiltClickThreshold)
	    {
	        if (this.tiltVector[0] > 0)
	        {
	            this.machine.dispatch('click_right');
	        } else {
	            this.machine.dispatch('click_left');
	        }
	    }
	    if (Math.abs(this.tiltVector[1]) > tiltClickThreshold)
	    {
	        if (this.tiltVector[1] > 0)
	        {
	            this.machine.dispatch('click_up');
	        } else {
	            this.machine.dispatch('click_down');
	        }
	    }
	    this.zoom += zoom;
        if (Math.abs(this.tiltVector[0]) > tiltClickThreshold)
        {
            if (this.zoom > 0)
            {
                this.machine.dispatch('click_in');
            } else {
                this.machine.dispatch('click_out');
            }
        }
        if (tiltVector[0]) {
            if (tiltVector[0] > 0)
            {
                this.machine.dispatch('slide_right');
            }  else {
               this.machine.dispatch('slide_left');
            }
        }
        if (tiltVector[1]) {
            if (tiltVector[1] > 0)
            {
                this.machine.dispatch('slide_up');
            } else {
                this.machine.dispatch('slide_down');
            }
        }
        if (zoom) {
            if (zoom > 0)
            {
                this.machine.dispatch('zoom_in');
            } else {
                this.machine.dispatch('zoom_out');
            }
        }


	},
	start: function()
	{
	    this.machine.dispatch('click');
	}
}

