
//for the initial canvas setup
var canvas = new fabric.Canvas('myCanvas');
canvas.backgroundColor = "white";
var redoOn = false;
var actionStack = [];
var holder;
var clipboard = null;

canvas.renderTop();
canvas.renderAll();

canvas.defaultCursor = 'circle';

canvas.on('object:added',function(){
  if(!redoOn){
    actionStack = [];
  }
  redoOn = false;
});
var color = "black";
var brushWidth = 2;





var fileInput = document.getElementById('img-inp');
fileInput.addEventListener('change', function(e){
    console.log("file input is real!");
    console.log(e);
    console.log(e.target.files[0].name);
    var fileReader = new FileReader();
    fileReader.onload = function(o) {
    console.log("yo");
        var imgObject = new Image();
        imgObject.src = o.target.result;
        imgObject.onload = function() {
            var img = new fabric.Image(imgObject);
            img.set({
                angle: 0,
                padding: 15,
                cornersize: 15,
                height: 200,
                width: 200
            });
            canvas.centerObject(img);
            canvas.add(img);
            canvas.renderAll();
        }
    }
    fileReader.readAsDataURL(e.target.files[0]);



});


// var bucket = document.getElementById("bkt");
// bucket.addEventListener('change',function (ev) {
//     // if(canvas.getActiveObject() === null){
//     //     canvas.setColor(ev.target.value);
//     // }
//     console.log(ev.target.value);
// });





function pencil() {
    if (!canvas.isDrawingMode) {
        canvas.isDrawingMode = true;
    }
    else {
        canvas.isDrawingMode = false;
    }
    canvas.defaultCursor = "/images/a.jpg";
    canvas.freeDrawingBrush.width = brushSize();
    canvas.freeDrawingBrush.color = color;
}



function addText() {
    var input = prompt("Enter your text: ");
    var text_obj = new fabric.Text(
        input,{
            fontFamily: 'Comic Sans',
            fontSize : 16,
            left: 100,
            top : 100
        }
    );

   // text_obj.on('selected', function(e){
   //     var x = prompt("change text: ");
   //     text_obj.setText(x);
   // });


    canvas.add(text_obj);

}




function eraser() {

}

function bucket() {
  var bucket = document.getElementById("col-pk").value;
  console.log(bucket == null);
  var selection = canvas.getActiveObject();
  if(selection == null){
      canvas.backgroundColor = bucket;
  }else{
      selection.set({fill:bucket});
      console.log("slection color has been changed to " + bucket);
  }
canvas.renderAll();


}



function select() {

}

function circle() {
    var circle = new fabric.Circle({
        radius: 20, fill: color, left: 100, top: 100
    });
    canvas.add(circle);
    canvas.renderAll();
}

function rectangle() {
    var rect = new fabric.Rect({
        width: 10, height: 20,
        left: 100, top: 100,
        fill: color,
        angle: 0
    });
    canvas.add(rect); // add Object
    canvas.renderAll();
}

function triangle() {
    var triangle = new fabric.Triangle({
        width: 20, height: 30, fill: color, left: 50, top: 50
    });
    canvas.add(triangle);
}

function setWidth(newWidth) {
    width = newWidth;
}

function setColor(newColor) {
    color = newColor;
}

function exportEdit() {
    holder = JSON.stringify(canvas.toJSON());
    console.log(holder);
    var fileName = window.prompt("Please enter filename:");
    var file = new Blob([holder], {type: "application/json"});
    if (window.navigator.msSaveOrOpenBlob) // IE10+
        window.navigator.msSaveOrOpenBlob(file, filename)
    else {
        var a = document.createElement("a"),
        url = URL.createObjectURL(file);
        a.href = url;
        a.download = fileName;
        document.body.appendChild(a);
        a.click();
        setTimeout(function() {
            document.body.removeChild(a);
            window.URL.revokeObjectURL(url);
        }, 0);
    }
}


var fileInput = document.getElementById('import-inp');
fileInput.addEventListener('change', function(e){
    console.log("file input is real!");
        console.log(e);
        console.log(e.target.files[0].name);
        var dataBlob;
        var fileReader = new FileReader();
        fileReader.onload = function(o) {
        console.log("yo");
        //canvas.clear();
        dataText = o.target.result;
        console.log(dataText);
        dataBlob = new Blob([dataText], {type: "application/json"});
        console.log(dataBlob);
        };
        //console.log(dataBlob);
        //var jsonText = fileReader.readAsText(dataBlob);
        //console.log(jsonText);
        //canvas.loadFromJSON(jsonText);
        console.log(fileReader.readAsText(e.target.files[0]));
        //canvas.loadFromJSON(fileReader.readAsText(e.target.files[0]),
        //canvas.renderAll.bind(canvas), function(o, object) {console.log(o, object)});

    //console.log("file input is real!");
    //console.log(e);
    //console.log(e.target.files[0].name);
    //var fileReader = new FileReader();
    //fileReader.onload = function(o) {
        //canvas.clear();
        //console.log("yo");
        //console.log(fileReader.readAsText(new Blob([o.target.result], {type: "application/json"})));
        //canvas.loadFromJSON(fileReader.readAsText(new Blob([o.target.result], {type: "application/json"})));
    });


function copy() {
    canvas.getActiveObject().clone(function(clonedObj) {
        clipboard = clonedObj;
    });
}

function cut() {
    canvas.getActiveObject().clone(function(clonedObj) {
        clipboard = clonedObj;
    });
    canvas.remove(canvas.getActiveObject());
}

function paste() {
    clipboard.clone(function(clonedObj) {
    	canvas.discardActiveObject();
    	clonedObj.set({
    	    left: clonedObj.left + 10,
    		top: clonedObj.top + 10,
    		event: true,
    	});
    	if (clonedObj.type === 'activeSelection') {
    		clonedObj.canvas = canvas;
    		clonedObj.forEachObject(function(o) {
    			canvas.add(o);
    		});
    		clonedObj.setCoords();
    	}
    	else {
    		canvas.add(clonedObj);
    	}
    	clipboard.top += 10;
    	clipboard.left += 10;
    	canvas.setActiveObject(clonedObj);
    	canvas.requestRenderAll();
    });
}



function undo() {
    if (canvas._objects.length > 0) {
        actionStack.push(canvas._objects.pop());
        canvas.renderAll();
    }
}

function redo() {
    if (actionStack.length > 0) {
        redoOn = true;
        canvas.add(actionStack.pop());
    }
}

function text() {
    var text = new fabric.IText('Type here...', { left: 100, top:100});
    canvas.add(text);
    canvas.renderAll();
}



function brushSize(){
    if (canvas.isDrawingMode) {
        var size = prompt("Enter the brush size: ");
    }
    return size;
}



function boldToggle(){

    var selected = canvas.getActiveObject();
    console.log(selected);
    if(selected.get('type')==='text' && selected.get('fontWeight')!== 'bold'){
        selected.set({fontWeight:'bold'});
    }else{
        selected.set({fontWeight:'normal'});
    }
    canvas.renderAll();
}


function italicToggle(){

    var selected = canvas.getActiveObject();
    console.log(selected);
    if(selected.get('type')==='text' && selected.get('fontStyle')!== 'italic'){
        selected.set({fontStyle:'italic'});
    }else{
        selected.set({fontStyle:'normal'});
    }

    canvas.renderAll();


}








function loadWorkspace(){

}


function initWorkspace(){

}


function registerHandler(){

}

function updateWorkspace(){

}




/**
 * need to add setup workspace component
 * need to register event listeners
 *
 */

