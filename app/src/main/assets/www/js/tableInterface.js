

const minScaler = 0.1;
const slideDivWidth = 1080;

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
  setMainDiv(newMainDiv) {
    this.mainDiv = newMainDiv;
    // if (this.mainDiv.classList.contains("hotspot_box") { do good things otherwise need to complain }
    this.slideShow = this.mainDiv.getElementsByClassName("hotspot_slide");
    if (this.slideShow.length > 0 )
    {
        defaultSlide = this.mainDiv.getElementById("0");
        if (defaultSlide == null || ! defaultSlide.classList.contains("hotspot_slide"))  {
            defaultSlide = this.slideShow[0];
        }
        defaultSlide.classList.add('left_slide');
        defaultSlide.style.display = "block";
    }

  },
  mainDiv: null,
  firstSlideInTray: null,
  player: null,
  state: 'idle',
  transitions: {
    'idle': {
      click: function () {
        this.changeStateTo('zoomingIn');
        this.dispatch('zoom_in');

      }
    },
    'zoomingIn': {
        fully_zoomed_in: function() {
            switch (this.slideShow.length) {
            case 0:
                this.changeStateTo('open');
                if (player)  player.play();
                break;
            case 1:
                this.currentSlide = this.mainDiv.getElementById("0");
                if (this.currentSlide == null || ! this.currentSlide.classList.contains("hotspot_slide"))  {
                    this.currentSlide = this.slideShow[0];
                }
                firstSlideInTray = 0;
                this.changeStateTo('paging');
                break;
            case 2:
                this.mainDiv.className = ""; // clears the list fast
                this.mainDiv.classList.add('hotspot_box');
                this.mainDiv.classList.add('two_slide_tray');
                this.currentSlide = this.mainDiv.getElementById("0");
                if (this.currentSlide == null || ! this.currentSlide.classList.contains("hotspot_slide"))  {
                    this.currentSlide = this.slideShow[0];
                }
                this.currentSlide.classList.add('left_slide');
                this.lastSlide = this.mainDiv.getElementById("1");
                if (this.lastSlide == null || ! this.lastSlide.classList.contains("hotspot_slide"))  {
                 this.lastSlide = this.slideShow[1];
                }
                this.lastSlide.classList.add('right_slide');
                this.mainDiv.left == 0;
                firstSlideInTray = 0;
                this.changeStateTo('paging');
                break;
            case 3:
            default:
                this.mainDiv.className = ""; // clears the list fast
                this.mainDiv.classList.add('hotspot_box');
                this.mainDiv.classList.add('three_slide_tray');
                this.currentSlide = this.mainDiv.getElementById("0");
                if (this.currentSlide == null || ! this.currentSlide.classList.contains("hotspot_slide"))  {
                    this.currentSlide = this.slideShow[0];
                }
                this.currentSlide.classList.remove('right_slide');
                this.currentSlide.classList.remove('center_slide');
                this.currentSlide.classList.add('left_slide');
                this.middleSlide = this.mainDiv.getElementById("1");
                if (this.middleSlide == null || ! this.middleSlide.classList.contains("hotspot_slide"))  {
                    this.middleSlide = this.slideShow[1];
                }
                this.middleSlide.classList.remove('left_slide');
                this.middleSlide.classList.remove('right_slide');
                this.middleSlide.classList.add('center_slide');
                this.lastSlide = this.mainDiv.getElementById("2");
                if (this.lastSlide == null || ! this.lastSlide.classList.contains("hotspot_slide"))  {
                 this.lastSlide = this.slideShow[21];
                }
                this.lastSlide.classList.remove('left_slide');
                this.lastSlide.classList.remove('center_slide');
                this.lastSlide.classList.add('right_slide');
                firstSlideInTray = 0;
                this.changeStateTo('paging');
                break;
         }
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
        Android.returnTableControlToMap();
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
            this.changeStateTo('paging');
            this.dispatch('slide_left');
        },
        click_right: function(){
            this.changeStateTo('paging');
            this.dispatch('slide_right');
        },
        click_up: function(){
            this.changeStateTo('paging');
            this.dispatch('slide_up');
        },
        click_down: function(){
            this.changeStateTo('paging');
            this.dispatch('slide_down');
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
        page_complete: function() {
        // this probably wants to implement a sort of detent to hold each slide for a moment

        },
        click_left: function(){
        },
        click_right: function(){
        },
        click_up: function(){
        },
        click_down: function(){
        },
        slide_left: function(){
            moveCurrentDivLeft();
            atMax = moveNextDivLeft();
            if (atMax)
            {
                this.dispatch('page_complete');
            }
        },
        slide_right: function(){
            moveCurrentDivRight();
            atMax = moveNextDivRight ();
            if (atMax)
            {
                this.dispatch('page_complete');
            }
        },
        slide_up: function(){
            moveCurrentDivUp();
            atMax = moveNextDivUp();
            if (atMax)
            {
                this.dispatch('page_complete');
            }
        },
        slide_down: function(){
            moveCurrentDivDown();
            atMax = moveNextDivDown();
            if (atMax)
            {
                this.dispatch('page_complete');
            }
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
    var currentTransform = machine.mainDiv.style.transform;
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
    machine.mainDiv.style.transform = currentTransform;
    return scaler;
}

function decreaseZoomOnMainDiv() {
    var currentTransform = machine.mainDiv.style.transform;
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
    machine.mainDiv.style.transform = currentTransform;
    return scaler;

}

function moveCurrentDivLeft()
{
    return pageDivs('left');
}

function  moveCurrentDivRight()
{
    return pageDivs('right');
}

function  moveCurrentDivUp()
{
    return pageDivs('up');
}

function  moveCurrentDivDown()
{
    return pageDivs('down');
}

function pageDivs(direction) {

    var done = true;
    const defaultIncrement = 1;
    var increment;
    var dir = 0; // 0 for horizontal, 1 for vertical
    switch (direction)
    {
        case 'up':
            increment = defaultIncrement;
            dir = 1;
            break;
        case 'down':
            increment = -defaultIncrement;
            dir = 1;
            break;
        case 'left':
            increment = -defaultIncrement;
            dir = 0;
            break;
        case 'right':
            increment = defaultIncrement;
            dir = 0;
            break;
        default:
            break;

    }

    switch (machine.slideShow.length)
    {
        case 0:
            break;
        case 1:
            break;
        case 2:
           done = false;
           if (dir === 0) {
                mainDiv.style.left += increment;
                mainDiv.style.left = Math.min(( mainDiv.style.width - slideDivWidth ), Math.max( 0, mainDiv.style.left ));
                if ( mainDiv.style.left === 0 || mainDiv.style.left === -slideDivWidth) done = true;
           }
           else {
                mainDiv.style.top += increment;
                mainDiv.style.top = Math.min(( mainDiv.style.height - slideDivWidth ), Math.max( 0, mainDiv.style.top ));
                if ( mainDiv.style.top === 0 || mainDiv.style.top === -slideDivWidth) done = true;
           }
            break;
        case 3:
            done = false;
            if (dir === 0) {
                 mainDiv.style.left += increment;
                 mainDiv.style.left = Math.min(( mainDiv.style.width - ( 2 * slideDivWidth )), Math.max( 0, mainDiv.style.left ));
                 if ( mainDiv.style.left === 0 || mainDiv.style.left === -slideDivWidth || mainDiv.style.left === -2 * slideDivWidth) done = true;
             }
             else {
                 mainDiv.style.top += increment;
                 mainDiv.style.top = Math.min(( mainDiv.style.height - ( 2 * slideDivWidth )), Math.max( 0, mainDiv.style.top ));
                 if ( mainDiv.style.top === 0 || mainDiv.style.top === -slideDivWidth || mainDiv.style.top === -2 * slideDivWidth) done = true;
            }
             break;

        default:
            done = false;
            if (dir === 0) {
                 mainDiv.style.left += increment;
                 mainDiv.style.left = Math.min(( mainDiv.style.width - ( 2 * slideDivWidth )), Math.max( 0, mainDiv.style.left ));
                 if ( mainDiv.style.left === 0 || mainDiv.style.left === -slideDivWidth || mainDiv.style.left === -2 * slideDivWidth) done = true;
             }
             else {
                 mainDiv.style.top += increment;
                 mainDiv.style.top = Math.min(( mainDiv.style.height - ( 2 * slideDivWidth )), Math.max( 0, mainDiv.style.top ));
                 if ( mainDiv.style.top === 0 || mainDiv.style.top === -slideDivWidth || mainDiv.style.top === -2 * slideDivWidth) done = true;
            }
            if (done) {
                switch ( mainDiv.style.left )
                {
                    case 0:
                    // if there's another slide to the left then push first two to right and then add new one on the right and move the tray div to center pos
                    if (machine.firstSlideInTray > 0) {
                        }
                    break;
                    case -slideDivWidth:
                    // no action
                    break;
                    case -2 * slideDivWidth:
                    // if there's another slide to the right then push last two to left and then add new one on the right
                    break;

                }
            }
             break;
    }

    /*
    move a three slide view accroding to tilt
*/
    return done;
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

