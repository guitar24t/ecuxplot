package org.nyet.ecuxplot;

import java.io.File;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.*;

import javax.swing.AbstractButton;
import javax.swing.JPanel;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;

import org.jfree.data.time.Month;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYSeries;

import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

import org.nyet.util.WaitCursor;
import org.nyet.util.GenericFileFilter;
import org.nyet.util.SubActionListener;

public class ECUxPlot extends ApplicationFrame implements SubActionListener {
    private ECUxDataset dataSet;
    private ChartPanel chart;
    private JMenuBar menuBar;
    private Comparable xkey="TIME";
    private AxisMenu xAxis;
    private AxisMenu yAxis;
    private AxisMenu yAxis2;

    private void setupAxisMenus(String[] headers) {
	if(xAxis!=null) this.menuBar.remove(xAxis);
	if(yAxis!=null) this.menuBar.remove(yAxis);
	if(yAxis2!=null) this.menuBar.remove(yAxis2);

	if(headers.length<=0) return;

	xAxis = new AxisMenu("X Axis", headers, this, true, "TIME");
	yAxis = new AxisMenu("Y Axis", headers, this, false, "RPM");
	yAxis2 = new AxisMenu("Y Axis2", headers, this, false, "BoostPressureActual");

	this.menuBar.add(xAxis);
	this.menuBar.add(yAxis);
	this.menuBar.add(yAxis2);
    }

    public void actionPerformed(ActionEvent event) {
	AbstractButton source = (AbstractButton) (event.getSource());
	if(source.getText().equals("Quit")) {
	    System.exit(0);
	} else if(source.getText().equals("Export Chart")) {
	    if(chart == null) {
		JOptionPane.showMessageDialog(this, "Open a CSV first");
	    } else {
		try {
		    this.chart.doSaveAs();
		} catch (Exception e) {
		    JOptionPane.showMessageDialog(this, e);
		    e.printStackTrace();
		}
	    }
	} else if(source.getText().equals("Close Chart")) {
	    this.dispose();
	} else if(source.getText().equals("New Chart")) {
	    final ECUxPlot plot = new ECUxPlot("ECUxPlot");
	    plot.pack();
	    Point where = this.getLocation();
	    where.translate(20,20);
	    plot.setLocation(where);
	    plot.setVisible(true);
	} else if(source.getText().equals("Open File")) {
	    //final JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
	    final JFileChooser fc = new JFileChooser();
	    fc.setFileFilter(new GenericFileFilter("csv", "CSV File"));
	    int ret = fc.showOpenDialog(this);
	    if(ret == JFileChooser.APPROVE_OPTION) {
		File file = fc.getSelectedFile();
		WaitCursor.startWaitCursor(this);
		try {
		    dataSet = new ECUxDataset(file.getAbsolutePath());
		    this.chart = CreateChartPanel(dataSet, this.xkey);
		    setContentPane(this.chart);
		    this.setTitle("ECUxPlot " + file.getName());
		    setupAxisMenus(dataSet.headers);
		} catch (Exception e) {
		    JOptionPane.showMessageDialog(this, e);
		    e.printStackTrace();
		}
		WaitCursor.stopWaitCursor(this);
	    }
	}
    }

    public void actionPerformed(ActionEvent event, Comparable parentId) {
	AbstractButton source = (AbstractButton) (event.getSource());
	// System.out.println(source.getText() + ":" + parentId);
	if(parentId.equals("X Axis")) {
	    this.xkey=source.getText();
	    ECUxChartFactory.setChartX(this.chart, dataSet, this.xkey);
	} else if(parentId.equals("Y Axis")) {
	    ECUxChartFactory.editChartY(this.chart, dataSet, this.xkey, source.getText(),0,source.isSelected());
	} else if(parentId.equals("Y Axis2")) {
	    ECUxChartFactory.editChartY(this.chart, dataSet, this.xkey, source.getText(),1,source.isSelected());
	}
    }

    private final class FileMenu extends JMenu {
	public FileMenu(String id, ActionListener listener) {
	    super(id);
	    JMenuItem openitem = new JMenuItem("Open File");
	    openitem.setAccelerator(KeyStroke.getKeyStroke(
		KeyEvent.VK_O, ActionEvent.CTRL_MASK));
	    openitem.addActionListener(listener);
	    this.add(openitem);

	    this.add(new JSeparator());

	    JMenuItem newitem = new JMenuItem("New Chart");
	    newitem.setAccelerator(KeyStroke.getKeyStroke(
		KeyEvent.VK_N, ActionEvent.CTRL_MASK));
	    newitem.addActionListener(listener);
	    this.add(newitem);

	    JMenuItem closeitem = new JMenuItem("Close Chart");
	    closeitem.setAccelerator(KeyStroke.getKeyStroke(
		KeyEvent.VK_W, ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK));
	    closeitem.addActionListener(listener);
	    this.add(closeitem);

	    this.add(new JSeparator());

	    JMenuItem exportitem = new JMenuItem("Export Chart");
	    exportitem.setAccelerator(KeyStroke.getKeyStroke(
		KeyEvent.VK_F4, ActionEvent.ALT_MASK));
	    exportitem.addActionListener(listener);
	    this.add(exportitem);

	    this.add(new JSeparator());

	    JMenuItem quititem = new JMenuItem("Quit");
	    quititem.setAccelerator(KeyStroke.getKeyStroke(
		KeyEvent.VK_F4, ActionEvent.ALT_MASK));
	    quititem.addActionListener(listener);
	    this.add(quititem);
	}
    }

    public ECUxPlot(final String title) {
        super(title);

	menuBar = new JMenuBar();

	FileMenu filemenu = new FileMenu("File", this);
	menuBar.add(filemenu);

	setJMenuBar(menuBar);

	setPreferredSize(new java.awt.Dimension(800,600));
    }

    private static ChartPanel CreateChartPanel(ECUxDataset data, Comparable xkey) throws Exception {
        final JFreeChart chart = ECUxChartFactory.create2AxisScatterPlot(
            "", // title
	    "", "", "", // xaxis, yaxis, yaxis2 label
            new DefaultXYDataset(), new DefaultXYDataset(),
	    PlotOrientation.VERTICAL,
            true,	// show legend
            true,	// show tooltips
            false	// show urls
        );

	ECUxChartFactory.setChartX(chart, data, xkey);
	ECUxChartFactory.addChartY(chart, data, xkey, "RPM", 0);
	ECUxChartFactory.addChartY(chart, data, xkey, "BoostPressureActual", 1);

        return new ChartPanel(chart);
    }

    public static void main(final String[] args) {
	final ECUxPlot plot = new ECUxPlot("ECUxPlot");
	plot.pack();
	RefineryUtilities.centerFrameOnScreen(plot);
	plot.setVisible(true);
    }
}