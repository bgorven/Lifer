$(document).ready(function () {
	
	/*
	 * The game board, which should have been preloaded by the page itself.
	 */
	if (window.hasOwnProperty("Board")) {
		var Board = window.Board;
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
		var table = board;
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
		var oldInfo = getTableInfo(document.getElementById("board"));
		
		var desiredWidth = ($(window).width()/oldInfo.cellSize + 2)|0;
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
	
	var hasCell = function(board, cell) {
		if (!board.hasOwnProperty("lookup")) {
			board.lookup = [];
			board.cells.forEach(function(i) {
				if (!board.lookup[i.x]) board.lookup[i.x] = [];
				board.lookup[i.x][i.y] = true;
			});
		}
		
		return board.lookup[cell.x] && board.lookup[cell.x][cell.y];
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
		        	generation: Board.generation || Board.generation === 0 ? 0 : -1
		        }
		    }).then(function(data) {
		       Board = data;
		       
		       //TODO: more and better ratelimiting.
		       //with this setup, we don't send multiple requests in parallel,
		       //but it's still not as smart as it could be.
		       refreshInProgress = false;
		       
		       //If the last refresh request comes in while a call is pending,
		       //we don't want it to get lost, and we don't want to shift the
		       //screen back from where it's been moved to in the mean time.
		       if (pendingRefreshInfo) {
		    	   refresh(pendingRefreshInfo);
		    	   pendingRefreshInfo = null;
		       } else {
			       repaint(tableInfo);
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
				} else if (CellsToAdd[loc.x] && CellsToAdd[loc.x][loc.y]) {
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
	var position = { x:0, y:0 };
	var onMove = function(event) {
		var table = getTableInfo(event.target);
		
		var calcX = calc(position.x, event.dx, table.cellSize);
		position.x = calcX.pos;
		table.x += calcX.shift;

		var calcY = calc(position.y, event.dy, table.cellSize);
		position.y = calcY.pos;
		table.y += calcY.shift;
		
		event.target.style.webkitTransform =
		      event.target.style.transform =
		        'translate(' + position.x + 'px, ' + position.y + 'px)';
		
		refresh(table);
	}
	
	var onTap = function(event) {
		var table = getTableInfo(document.getElementById("board"));
		var loc = {
				x: ((event.pageX - position.x)/table.cellSize + Left)|0,
				y: ((event.pageY - position.y)/table.cellSize + Top)|0
		}
		
		if (!hasCell(Board, loc)) {
			if (!CellsToAdd[loc.x]) CellsToAdd[loc.x] = [];
			
			if (CellsToAdd[loc.x][loc.y]) {
				CellsToAdd[loc.x][loc.y] = false;
			} else {
				CellsToAdd[loc.x][loc.y] = true;
			}
		}
		
		repaint(table);
	}
	
	var addCells = function() {
		var cells = [];
		CellsToAdd.forEach(function(col, x) {
			col.forEach(function(cell, y) {
				if (cell) {
					cells.push({
						x: x,
						y: y
					});
				}
			});
		});
		
		$.ajax({
			type: "POST",
		    url: "/board",
		    data: JSON.stringify( { cells: cells, generation: Board.generation } ),
		    contentType: "application/json; charset=utf-8",
		    dataType: "json",
		}).then(function(data) {
			if (data === true) {
				CellsToAdd = [];
				refresh(getTableInfo(document.getElementById("board")));
			}
		});
	}

	
	onResize();
	$(window).resize(onResize);
	$("#addBtn").click(addCells).prop("type", "button");
	interact("#board")
		.draggable({
			inertia: true,
			onmove: onMove
		})
		.on("tap", onTap);
});
