
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
        zoom_in: function(){
        },
        zoom_out: function(){
        this.changeStateTo('zooming_out');
        this.dispatch('zoom_out');
        }
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
    if (currentTransform.indexOf('scale(' == 0))
    {
        scaler = currentTransform.substring(currentTransform.indexOf('(') +1, currentTransform.indexOf(')'));
        if (scaler.indexOf(',') > 0) scaler = scaler.substring(0,scaler.indexOf(','));
        scaler = Math.min(1.0, Number(scaler) + zoomIncrement);
        currentTransform = 'scale(' + scaler +');'
    }
}

var TableInterface = {
    constructor: function(mainDiv) {
		this.tiltVector = [0.0, 0.0];
		this.zoom = 0.0;
		this.mainDiv = mainDiv;
		this.fsm = machine;
	},
	update: function update(tiltVector, zoom) {
	    this.tiltVector = [this.tiltVector[0] + tiltVector[0], this.tiltVector[1] + tiltVector[1]];
	},
	start: function()
	{
	    this.machine.dispatch('click');
	}
}

