package it.achdjian.plugin.esp32.ui

import java.awt.Container
import java.awt.Dimension
import java.awt.GridLayout


class GridLayout2(row: Int, col: Int) : GridLayout(row, col, 0, 0) {

    override fun preferredLayoutSize(parent: Container): Dimension {
        synchronized(parent.treeLock) {
            val insets = parent.insets
            val nComponents = parent.componentCount
            var nRows = rows
            var nCols = columns
            if (nRows > 0) {
                nCols = (nComponents + nRows - 1) / nRows
            } else {
                nRows = (nComponents + nCols - 1) / nCols
            }
            val w = IntArray(nCols)
            val h = IntArray(nRows)
            for (i in 0 until nComponents) {
                val r = i / nCols
                val c = i % nCols
                val comp = parent.getComponent(i)
                val d = comp.preferredSize
                if (w[c] < d.width) {
                    w[c] = d.width
                }
                if (h[r] < d.height) {
                    h[r] = d.height
                }
            }
            val nw = w.sum()
            val nh = h.sum()
            return Dimension(insets.left + insets.right + nw + (nCols - 1) * hgap,
                    insets.top + insets.bottom + nh + (nRows - 1) * vgap)
        }
    }


    override fun minimumLayoutSize(parent: Container): Dimension {
        System.err.println("minimumLayoutSize")
        synchronized(parent.treeLock) {
            val insets = parent.insets
            val nComponents = parent.componentCount
            var nRows = rows
            var nCols = columns
            if (nRows > 0) {
                nCols = (nComponents + nRows - 1) / nRows
            } else {
                nRows = (nComponents + nCols - 1) / nCols
            }
            val w = IntArray(nCols)
            val h = IntArray(nRows)
            for (i in 0 until nComponents) {
                val r = i / nCols
                val c = i % nCols
                val comp = parent.getComponent(i)
                val d = comp.minimumSize
                if (w[c] < d.width) {
                    w[c] = d.width
                }
                if (h[r] < d.height) {
                    h[r] = d.height
                }
            }
            val nw = w.sum()
            val nh = h.sum()
            return Dimension(insets.left + insets.right + nw + (nCols - 1) * hgap,
                    insets.top + insets.bottom + nh + (nRows - 1) * vgap)
        }
    }

    override fun layoutContainer(parent: Container) {
        synchronized(parent.treeLock) {
            val insets = parent.insets
            val nComponents = parent.componentCount
            var nRows = rows
            var nCols = columns
            if (nComponents == 0) {
                return
            }
            if (nRows > 0) {
                nCols = (nComponents + nRows - 1) / nRows
            } else {
                nRows = (nComponents + nCols - 1) / nCols
            }
            val hgap = hgap
            val vgap = vgap
            val w = IntArray(nCols)
            val h = IntArray(nRows)
            for (i in 0 until nComponents) {
                val r = i / nCols
                val c = i % nCols
                val comp = parent.getComponent(i)
                val d = comp.preferredSize
                if (w[c] < d.width) {
                    w[c] = d.width
                }
                if (h[r] < d.height) {
                    h[r] = d.height
                }
            }
            var x = insets.left
            for (c in 0 until nCols) {
                var y = insets.top
                for (r in 0 until nRows) {
                    val componentIndex = r * nCols + c
                    if (componentIndex < nComponents) {
                        val component = parent.getComponent(componentIndex)
                        component.setBounds(x, y, w[c], h[r])
                    }
                    y += h[r] + vgap
                }
                x += w[c] + hgap
            }
        }
    }
}