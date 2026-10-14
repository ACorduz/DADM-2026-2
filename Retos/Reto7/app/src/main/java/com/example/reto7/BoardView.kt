package com.example.reto7

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View

/**
 * Custom View that draws the tic-tac-toe board: a 3x3 grid of light gray
 * lines with the X and O bitmaps drawn into the occupied cells. It reads
 * the board state from the TicTacToeGame given to setGame().
 */
class BoardView : View {

    companion object {
        // Width of the board grid lines
        const val GRID_WIDTH = 6
    }

    private lateinit var mHumanBitmap: Bitmap
    private lateinit var mComputerBitmap: Bitmap
    private lateinit var mPaint: Paint

    private var mGame: TicTacToeGame? = null

    // Destination rectangle for the X/O images, reused on every draw
    private val mDestRect = Rect()

    constructor(context: Context) : super(context) {
        initialize()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        initialize()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initialize()
    }

    fun initialize() {
        mHumanBitmap = BitmapFactory.decodeResource(resources, R.drawable.x_img)
        mComputerBitmap = BitmapFactory.decodeResource(resources, R.drawable.o_img)
        mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    }

    fun setGame(game: TicTacToeGame) {
        mGame = game
    }

    // Touches are handled by MainActivity's OnTouchListener, which calls
    // this so accessibility services see the touch as a click.
    override fun performClick(): Boolean = super.performClick()

    fun getBoardCellWidth(): Int = width / 3

    fun getBoardCellHeight(): Int = height / 3

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Determine the width and height of the View
        val boardWidth = width
        val boardHeight = height

        // Make thick, light gray lines
        mPaint.color = Color.LTGRAY
        mPaint.strokeWidth = GRID_WIDTH.toFloat()

        // Draw the two vertical board lines
        val cellWidth = boardWidth / 3
        canvas.drawLine(cellWidth.toFloat(), 0f, cellWidth.toFloat(), boardHeight.toFloat(), mPaint)
        canvas.drawLine(cellWidth * 2f, 0f, cellWidth * 2f, boardHeight.toFloat(), mPaint)

        // Draw the two horizontal board lines
        val cellHeight = boardHeight / 3
        canvas.drawLine(0f, cellHeight.toFloat(), boardWidth.toFloat(), cellHeight.toFloat(), mPaint)
        canvas.drawLine(0f, cellHeight * 2f, boardWidth.toFloat(), cellHeight * 2f, mPaint)

        // Draw all the X and O images
        val game = mGame ?: return
        for (i in 0 until TicTacToeGame.BOARD_SIZE) {
            val col = i % 3
            val row = i / 3

            // Define the boundaries of a destination rectangle for the image:
            // the cell, shrunk by the grid line width so the image doesn't
            // overlap the lines.
            val left = col * cellWidth + GRID_WIDTH
            val top = row * cellHeight + GRID_WIDTH
            val right = (col + 1) * cellWidth - GRID_WIDTH
            val bottom = (row + 1) * cellHeight - GRID_WIDTH
            mDestRect.set(left, top, right, bottom)

            when (game.getBoardOccupant(i)) {
                TicTacToeGame.HUMAN_PLAYER -> canvas.drawBitmap(
                    mHumanBitmap,
                    null, // src
                    mDestRect, // dest
                    null
                )
                TicTacToeGame.COMPUTER_PLAYER -> canvas.drawBitmap(
                    mComputerBitmap,
                    null, // src
                    mDestRect, // dest
                    null
                )
            }
        }
    }
}
