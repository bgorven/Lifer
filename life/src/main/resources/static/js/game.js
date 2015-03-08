$(document).ready(function () {
	
	/*
	 * The game board, which should have been preloaded by the page itself.
	 */
	if (window.hasOwnProperty('Lifer')) {
		var Board = window.Lifer.board;
	} else {
		var Board = {
				x: 0,
				y: 0,
				width: 0,
				height: 0,
				generation: -1,
				cells: []
		}
	}
	
	/*
	 * Pending changes to be posted to the server
	 */
	var CellsToAdd = [];
	
	/*
	 * the leftmost cell currently shown on screen
	 */
	var Left = Board.x;
	/*
	 * the topmost cell currently shown on screen
	 */
	var Top = Board.y;
	
	/*
	 * Extracts info about the DOM element containing the game board
	 */
	var getTableInfo = function(board) {
		var table = board || document.getElementById("board");
		var body = table.getElementsByTagName("tbody")[0];
		var rows = body.getElementsByTagName("tr");
		var exampleRow = rows[0].getElementsByTagName("td");
		return {
			table: table,
			body: body,
			rows: rows,
			exampleRow: exampleRow,
			cellSize: exampleRow[0].offsetWidth,
			x: Left,
			y: Top,
			width: exampleRow.length,
			height: rows.length,
		};
	};
	
	/*
	 * Creates a new DOM tree to replace the game board with
	 */
	var newTableInfo = function(x, y, width, height) {
		width = width|0;
		height = height|0;
		var result = {
			body: document.createElement("tbody"),
			rows: [],
			x: x,
			y: y,
			width: width,
			height: height,
		};
		
		for (var y = 0; y < height; y++) {
			var tr = document.createElement("tr");
			result.body.appendChild(tr);
			result.rows.push(tr);
			
			
			for (var x = 0; x < width; x++) {
				tr.appendChild(document.createElement("td"));
			}
		}
		
		return result;
	};
	
	/*
	 * Add or remove cells to fill the viewport.
	 */  
	var onResize = function() {
		var oldInfo = getTableInfo();
		
		var desiredWidth = $(window).width();
		desiredWidth /= oldInfo.cellSize;
		desiredWidth += 2;
		desiredWidth |= 0;
		var desiredHeight = ($(window).height()/oldInfo.cellSize + 2)|0;
		
		if (desiredWidth !== oldInfo.width || desiredHeight !== oldInfo.height) {
			var center = {
					x: oldInfo.x + oldInfo.width/2,
					y: oldInfo.y + oldInfo.height/2
			};
			var newLoc = {
					x: Math.round(center.x - desiredWidth/2),
					y: Math.round(center.y - desiredHeight/2)
			};
			var tableInfo = newTableInfo(newLoc.x, newLoc.y, desiredWidth, desiredHeight);
			
			Left = newLoc.x;
			Top = newLoc.y;
			refresh(tableInfo);
			
			oldInfo.table.replaceChild(tableInfo.body, oldInfo.body);
		}
	};
	
	var hasCell = function(board, cell){
		if (Array.isArray(board)) board = { cells: board };
		for (var i = 0; i < board.cells.length; i++){
			if (board.cells[i].x === cell.x && board.cells[i].y === cell.y) return true;
		}
		return false;
	} 
	
	
	var contains = function(board, point) {
		return ((point.x >= board.x && point.y >= board.y) && 
				(point.x < (board.x + board.width) && point.y < (board.y + board.height)));
	}
	
	/*
	 * Updates the ui with best known information, requests fresh data if necessary,
	 * then updates the ui again when it arrives.
	 */
	var refreshInProgress = false;
	var pendingRefreshInfo = null;
	var refresh = function(tableInfo) {
		if (refreshInProgress) {
			pendingRefreshInfo = tableInfo;
		} else {
		
			refreshInProgress = true;
			
			$.ajax({
		        url: "/board",
		        data: {
		        	x: tableInfo.x,
		        	y: tableInfo.y,
		        	width: tableInfo.width,
		        	height: tableInfo.height,
		        	generation: Board.generation
		        }
		    }).then(function(data) {
		       Board = data;
		       
		       repaint(tableInfo);
		       
		       //TODO: more and better ratelimiting.
		       //with this setup, we don't send multiple requests in parallel,
		       //but it's still not as smart as it could be.
		       refreshInProgress = false;
		       
		       //If the last refresh request comes in while a call is pending,
		       //we don't want it to get lost.
		       if (pendingRefreshInfo) {
		    	   refresh(pendingRefreshInfo);
		    	   pendingRefreshInfo = null;
		       }
		    });
		}
		
		//repaint while waiting for ajax request to return
		//(highlights unknown cells until the data arrives)
		Left = tableInfo.x;
		Top = tableInfo.y;
		repaint(tableInfo);
	}
	
	/*
	 * Draws cells on the grid.
	 */
	var repaint = function(tableInfo) {
		
		for (var j = 0; j < tableInfo.rows.length; j++) {
			var row = tableInfo.rows[j].getElementsByTagName("td");
			
			for (var i = 0; i < row.length; i++) {
				var loc = {
						x: Left + i,
						y: Top + j
					};
				
				if (hasCell(Board, loc)){
					setImage(row[i], "img/alive.png");
				} else if (hasCell(CellsToAdd, loc)) {
					setImage(row[i], "img/pending.png");
				} else if (!contains(Board, loc)) {
					setImage(row[i], "img/unknown.png");
				} else {
					while (row[i].firstChild) {
						row[i].removeChild(row[i].firstChild);
					}
				}
			}
		}
	}
	
	var setImage = function(elem, imgSrc) {
		if (elem.hasChildNodes()) {
			var asdf = elem.firstChild.getAttribute("src");
			var fdsa = asdf === imgSrc;
			if (elem.firstChild.tagName !== "IMG" 
				|| elem.firstChild.getAttribute("src") !== imgSrc) {

				var img = document.createElement("img");
				img.setAttribute("src", imgSrc);
				elem.replaceChild(img, elem.firstChild);
			}
		} else {
			var img = document.createElement("img");
			img.setAttribute("src", imgSrc);
			elem.appendChild(img);
		}
	}

	/*
	 * Rather than changing the dom when panning, we simulate an infinite canvas
	 * by having a table that extends one cell off-screen in any direction, and 
	 * wrapping each time the table moves by more than one cell.
	 * 
	 * This function gets the new location for the table and the amount to shift 
	 * the board by, given the table's initial position, the distance moved, and
	 * the size of a cell.
	 */
	var calc = function(position, delta, size) {
		var pos = position + delta;
		var result = { 
				shift: 0,
				pos: pos
		};
		//If pos is positive, the lowest index has moved toward the middle,
		//the the index will change by a negative number (shift), and position
		//will change to a number between -size and 0.
		if (pos > 0) {
			result.shift = -(Math.floor(pos / size) + 1);
		} else {
			result.shift = Math.floor(-pos / size);
		}
		result.pos = pos + result.shift * size;
		
		return result;
	}
	
	/*
	 * when the window is moved, repaint immediately, and if some cells are unknown,
	 * send an ajax request for the rest of the data.
	 */
	var position = $("#board").position();
	var onMove = function(event) {
		var table = getTableInfo(event.target);
		
		var calcX = calc(
				position.left,
				event.dx, 
				table.cellSize);
		event.target.style.top = calcX.pos + "px";
		position.left = calcX.pos;
		table.x += calcX.shift;

		var calcY = calc(
				position.top,
				event.dy, 
				table.cellSize);
		event.target.style.top = calcY.pos + "px";
		position.top = calcY.pos;
		table.y += calcY.shift;
		
		refresh(table);
	}

	
	onResize();
	$(window).resize(onResize);
	interact("#board")
		.draggable({
			inertia: true,
			onmove: onMove
		});
});
