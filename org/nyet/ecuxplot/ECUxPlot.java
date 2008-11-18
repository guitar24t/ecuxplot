package org.nyet.ecuxplot;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.prefs.Preferences;

import java.awt.Color;
import java.awt.Point;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

import javax.swing.*;

import com.apple.eawt.*;

import org.jfree.chart.JFreeChart;

import org.jfree.data.time.Month;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYSeries;

import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

import org.nyet.util.WindowUtilities;
import org.nyet.util.WaitCursor;
import org.nyet.util.GenericFileFilter;
import org.nyet.util.SubActionListener;
import org.nyet.util.Files;

import org.nyet.logfile.Dataset;

public class ECUxPlot extends ApplicationFrame implements SubActionListener {
    // each file loaded has an associated dataset
    private HashMap<String, ECUxDataset> fileDatasets =
	    new HashMap<String, ECUxDataset>();

    private ECUxChartPanel chartPanel;

    // Menus
    private JMenuBar menuBar;
    private AxisMenu xAxis;
    private AxisMenu yAxis[] = new AxisMenu[2];

    // Dialog boxes
    private JFileChooser fc;
    private FilterEditor fe;
    private ConstantsEditor ce;
    private PIDEditor pe;
    private FuelingEditor fle;
    private SAEEditor sae;

    // Preferences
    private Preferences prefs=null;

    private Env env;
    private Filter filter;

    public static boolean scatter(Preferences prefs) {
	return prefs.getBoolean("scatter", false);
    }

    private boolean scatter() {
	return this.scatter(this.prefs);
    }

    private Comparable xkey() {
	final String defaultXkey = "RPM";
	return this.prefs.get("xkey", defaultXkey);
    }

    private Comparable[] ykeys(int index) {
	final String[] defaultYkeys = {
	    "Calc WHP,Calc WTQ",
	    "BoostPressureDesired (PSI),BoostPressureActual (PSI)"
	};

	String k=this.prefs.get("ykeys"+index, defaultYkeys[index]);
	return k.split(",");
    }

    private java.awt.Dimension windowSize() {
	return new java.awt.Dimension(
	    this.prefs.getInt("windowWidth", 800),
	    this.prefs.getInt("windowHeight", 600));
    }

    private void putWindowSize() {
	this.prefs.putInt("windowWidth", this.getWidth());
	this.prefs.putInt("windowHeight", this.getHeight());
    }

    private void putYkeys(int axis) {
	final org.jfree.chart.plot.XYPlot plot =
	    this.chartPanel.getChart().getXYPlot();
	DefaultXYDataset dataset = (DefaultXYDataset)plot.getDataset(axis);
	this.prefs.put("ykeys"+axis,ECUxChartFactory.getDatasetYkeys(dataset));
    }

    private void setupAxisMenus(String[] headers) {
	if(this.xAxis!=null) this.menuBar.remove(this.xAxis);
	if(this.yAxis[0]!=null) this.menuBar.remove(this.yAxis[0]);
	if(this.yAxis[1]!=null) this.menuBar.remove(this.yAxis[1]);

	if(headers.length<=0) return;

	this.xAxis = new AxisMenu("X Axis", headers, this, true, this.xkey());
	this.menuBar.add(xAxis, 2);

	this.yAxis[0] = new AxisMenu("Y Axis", headers, this, false,
	    this.ykeys(0));
	this.menuBar.add(yAxis[0], 3);

	this.yAxis[1] = new AxisMenu("Y Axis2", headers, this, false,
	    this.ykeys(1));
	this.menuBar.add(yAxis[1], 4);
    }

    private void loadFile(File file) { loadFile(file, false); }
    private void loadFile(File file, Boolean replace) {
	try {
	    ECUxDataset data = new ECUxDataset(file.getAbsolutePath(),
		    this.env, this.filter);

	    // replacing, nuke all the currently loaded datasets
	    if(replace)
		this.fileDatasets = new HashMap<String, ECUxDataset>();

	    this.fileDatasets.put(file.getName(), data);
	    this.setTitle("ECUxPlot " + fileDatasets.keySet().toString());

	    final JFreeChart chart =
		ECUxChartFactory.create2AxisChart(this.scatter());

	    this.chartPanel = new ECUxChartPanel(chart);

	    setContentPane(this.chartPanel);
	    rebuild();
	    addChartY(this.ykeys(0), 0);
	    addChartY(this.ykeys(1), 1);

	    Iterator itc = this.fileDatasets.values().iterator();
	    HashSet<String> hset = new HashSet<String>();
	    while(itc.hasNext()) {
		String h[] = ((ECUxDataset)itc.next()).getHeaders();
		for(int i = 0; i<h.length; i++)
		    hset.add(h[i]);
	    }
	    setupAxisMenus(hset.toArray(new String[0]));
	} catch (Exception e) {
	    JOptionPane.showMessageDialog(this, e);
	    e.printStackTrace();
	    return;
	}
    }

    public void actionPerformed(ActionEvent event) {
	AbstractButton source = (AbstractButton) (event.getSource());
	if(source.getText().equals("Quit")) {
	    exitApp();
	} else if(source.getText().equals("Export Chart")) {
	    if(this.chartPanel == null) {
		JOptionPane.showMessageDialog(this, "Open a CSV first");
	    } else {
		try {
		    String stem=null;
		    Iterator itc = this.fileDatasets.values().iterator();
		    while(itc.hasNext()) {
			String fname=((ECUxDataset)itc.next()).getFilename();
			if(stem == null) {
			    stem = Files.stem(fname);
			} else {
			    stem += "_vs_" + Files.stem(Files.filename(fname));
			}
		    }
		    this.chartPanel.doSaveAs(stem);
		} catch (Exception e) {
		    JOptionPane.showMessageDialog(this, e);
		    e.printStackTrace();
		}
	    }
	} else if(source.getText().equals("Clear Chart")) {
	    // nuke axis menus
	    this.menuBar.remove(this.xAxis);
	    this.menuBar.remove(this.yAxis[0]);
	    this.menuBar.remove(this.yAxis[1]);
	    this.xAxis = null;
	    this.yAxis = new AxisMenu[2];
	    // nuke datasets
	    this.fileDatasets = new HashMap<String, ECUxDataset>();
	    this.setTitle("ECUxPlot");
	    this.chartPanel.setChart(null);
	} else if(source.getText().equals("Close Chart")) {
	    this.dispose();
	} else if(source.getText().equals("New Chart")) {
	    final ECUxPlot plot = new ECUxPlot("ECUxPlot");
	    plot.pack();
	    Point where = this.getLocation();
	    where.translate(20,20);
	    plot.setLocation(where);
	    plot.setVisible(true);
	} else if(source.getText().equals("Open File") || 
		  source.getText().equals("Add File") ) {
	    if(fc==null) {
		// current working dir
		// String dir  = System.getProperty("user.dir"));
		// home dir
		String dir = this.prefs.get("chooserDir",
		    System.getProperty("user.home"));
		fc = new JFileChooser(dir);
		fc.setFileFilter(new GenericFileFilter("csv", "CSV File"));
	    }
	    int ret = fc.showOpenDialog(this);
	    if(ret == JFileChooser.APPROVE_OPTION) {
		Boolean replace =
		    source.getText().equals("Open File")?true:false;

		WaitCursor.startWaitCursor(this);
		loadFile(fc.getSelectedFile(), replace);
		WaitCursor.stopWaitCursor(this);
		this.prefs.put("chooserDir",
		    fc.getCurrentDirectory().toString());
	    }
	} else if(source.getText().equals("Scatter plot")) {
	    boolean s = source.isSelected();
	    this.prefs.putBoolean("scatter", s);
	    if(this.chartPanel != null)
		ECUxChartFactory.setChartStyle(this.chartPanel.getChart(),
		    !s, s);
	} else if(source.getText().equals("Filter data")) {
	    this.filter.enabled(source.isSelected());
	    rebuild();
	} else if(source.getText().equals("Configure filter...")) {
	    if(this.fe == null) this.fe = new FilterEditor(this.prefs);
	    this.fe.showDialog(this, "Filter", this.filter);
	} else if(source.getText().equals("Edit constants...")) {
	    if(this.ce == null) this.ce = new ConstantsEditor(this.prefs);
	    this.ce.showDialog(this, "Constants", this.env.c);
	} else if(source.getText().equals("Edit fueling...")) {
	    if(this.fle == null) this.fle = new FuelingEditor(this.prefs);
	    this.fle.showDialog(this, "Fueling", this.env.f);
	} else if(source.getText().equals("Edit PID...")) {
	    if(this.pe == null) this.pe = new PIDEditor();
	    this.pe.showDialog(this, "PID", this.env.pid);
	} else if(source.getText().equals("Apply SAE")) {
	    this.env.sae.enabled(source.isSelected());
	    rebuild();
	    updateLabelTitle();
	} else if(source.getText().equals("Edit SAE constants...")) {
	    if(this.sae == null) this.sae = new SAEEditor(this.prefs);
	    this.sae.showDialog(this, "SAE", this.env.sae);
	} else if(source.getText().equals("About...")) {
	    JPanel info = new JPanel();
	    info.setLayout(new BorderLayout());
	    info.add(new JLabel((new org.nyet.util.Version()).toString()),
		BorderLayout.NORTH);
	    JButton url = new JButton(
    "<html><a href=\"http://nyet.org/cars/ECUxPlot\">ECUxPlot home page</a>"
	    );
	    url.setActionCommand("Homepage");
	    url.setBorderPainted(false);
	    url.addActionListener(this);
	    info.add(url, BorderLayout.CENTER);
	    JOptionPane.showMessageDialog(this, info,
		    "About ECUxPlot", JOptionPane.PLAIN_MESSAGE);
	} else if("Homepage".equals(event.getActionCommand())) {
	    boolean error = true;
	    if (java.awt.Desktop.isDesktopSupported()) {
		try {
		    java.awt.Desktop.getDesktop().browse(
			    new java.net.URI("http://nyet.org/cars/ECUxPlot"));
		    error = false;
		} catch (Exception e) {
		}
	    }
	    if (error)
		JOptionPane.showMessageDialog(this,
	    "Can't launch browser. Please download the latest JRE from Sun.");
	} else {
	    System.out.println("unhandled getText=" + source.getText() +
		   ", actionCommand=" + event.getActionCommand());
	}
    }

    private String findUnits(Comparable key) {
	Iterator itc = this.fileDatasets.values().iterator();
	while(itc.hasNext()) {
	    try {
		String units = ((ECUxDataset)itc.next()).units(key);
		// return the first one for now.
		if(units!=null) return units;
	    } catch (Exception e) {
	    }
	}
	return "";
    }

    private void updateLabelTitle() {
	final org.jfree.chart.plot.XYPlot plot =
	    this.chartPanel.getChart().getXYPlot();
	String title = "";
	for(int axis=0; axis<plot.getDatasetCount(); axis++) {
	    final org.jfree.data.xy.XYDataset dataset = plot.getDataset(axis);
	    String seriesTitle=null, sprev=null;
	    String label="", lprev=null;
	    for(int i=0; dataset!=null && i<dataset.getSeriesCount(); i++) {
		Comparable key = dataset.getSeriesKey(i);
		if(key==null) continue;
		String s;

		if(key instanceof Dataset.Key)
		    s = ((Dataset.Key)key).getString();
		else
		    s = key.toString();
		
		String l = findUnits(key);

		if(sprev==null || !s.equals(sprev)) {
		    if(seriesTitle == null) 
			seriesTitle = s;
		    else
			seriesTitle += ", " + s;
		}
		sprev=s;

		if(l==null) continue;
		if(lprev==null || !l.equals(lprev)) {
		    if(!label.equals("")) label += ", ";
		    label += l;
		}
		lprev=l;
	    }
	    if(seriesTitle != null && !seriesTitle.equals("")) {
		if(!title.equals("")) title += " and ";
		title += seriesTitle;
	    }
	    plot.getRangeAxis(axis).setLabel(label);
	    /* hide axis if this axis has no series */
	    plot.getRangeAxis(axis).setVisible(dataset.getSeriesCount()>0);
	}
	this.chartPanel.getChart().setTitle(title);
    }

    public void rebuild() {
	if(this.chartPanel==null) return;

	final org.jfree.chart.plot.XYPlot plot =
	    this.chartPanel.getChart().getXYPlot();

	WaitCursor.startWaitCursor(this);
	for(int i=0;i<plot.getDatasetCount();i++) {
	    org.jfree.data.xy.XYDataset pds = plot.getDataset(i);
	    final DefaultXYDataset newdataset = new DefaultXYDataset();
	    for(int j=0;j<pds.getSeriesCount();j++) {
		Dataset.Key ykey = (Dataset.Key)pds.getSeriesKey(j);
		if(this.fileDatasets.size()==1) ykey.hideFilename();
		else ykey.showFilename();

		ECUxDataset data = this.fileDatasets.get(ykey.getFilename());
		ECUxChartFactory.addDataset(newdataset, data,
		    this.xkey(), ykey);
	    }
	    plot.setDataset(i, newdataset);
	}

	// find x axis label. just pick first one that has units we can use
	String label = "";
	Iterator itc = this.fileDatasets.values().iterator();
	while(itc.hasNext()) {
	    ECUxDataset data = (ECUxDataset) itc.next();
	    if(data.get(this.xkey())!=null) {
		String units = data.units(this.xkey());
		if(units != null) {
		    label = this.xkey().toString() + " ("+units+")";
		    break;
		}
	    }
	}

	plot.getDomainAxis().setLabel(label);
	WaitCursor.stopWaitCursor(this);
    }

    private void removeAll(int axis) {
	final org.jfree.chart.plot.XYPlot plot =
	    this.chartPanel.getChart().getXYPlot();
	ECUxChartFactory.removeDataset((DefaultXYDataset)plot.getDataset(axis));
    	this.yAxis[axis].uncheckAll();
    }

    private void editChartY(Comparable ykey, int axis, boolean add) {
	Iterator itc = this.fileDatasets.values().iterator();
	while(itc.hasNext()) {
	    editChartY((ECUxDataset)itc.next(), ykey, axis, add);
	}
    }

    private void editChartY(ECUxDataset data, Comparable ykey, int axis,
	boolean add) {
	if(add && !(data.exists(ykey)) )
	    return;
	final org.jfree.chart.plot.XYPlot plot =
	    this.chartPanel.getChart().getXYPlot();
	DefaultXYDataset pds = (DefaultXYDataset)plot.getDataset(axis);
	if(add) {
	    Dataset.Key key = data.new Key(data.getFilename(),
		    ykey.toString());
	    if(this.fileDatasets.size()==1) key.hideFilename();
	    ECUxChartFactory.addDataset(pds, data, this.xkey(), key);
	} else {
	    ECUxChartFactory.removeDataset(pds, ykey);
	}
    }

    private void addChartY(Comparable[] ykey, int axis) {
	for(int i=0; i<ykey.length; i++)
	    editChartY(ykey[i], axis, true);
	updateLabelTitle();
    }

    public void actionPerformed(ActionEvent event, Comparable parentId) {
	AbstractButton source = (AbstractButton) (event.getSource());
	// System.out.println(source.getText() + ":" + parentId);
	if(parentId.equals("X Axis")) {
	    this.prefs.put("xkey",source.getText());
	    /* rebuild depends on the value of prefs */
	    rebuild();
	} else if(parentId.equals("Y Axis")) {
	    if(source.getText().equals("Remove all")) {
		removeAll(0);
	    } else {
		editChartY(source.getText(),0,source.isSelected());
	    }
	    /* putkeys depends on the stuff that edit chart does */
	    putYkeys(0);
	} else if(parentId.equals("Y Axis2")) {
	    if(source.getText().equals("Remove all")) {
		removeAll(1);
	    } else {
		editChartY(source.getText(),1,source.isSelected());
	    }
	    /* putkeys depends on the stuff that edit chart does */
	    putYkeys(1);
	}
	updateLabelTitle();
    }

    // Constructor with args
    public ECUxPlot(final String title, String[] args) {
        super(title);
	WindowUtilities.setNativeLookAndFeel();
	this.menuBar = new JMenuBar();

	this.prefs = Preferences.userNodeForPackage(ECUxPlot.class);

	this.filter = new Filter(this.prefs);
	this.env = new Env(this.prefs);

	java.net.URL imageURL =
	    getClass().getResource("icons/ECUxPlot2-64.png");

	if(imageURL==null) {
	    System.out.println("cant open icon");
	    System.exit(0);
	}
	this.setIconImage(new javax.swing.ImageIcon(imageURL).getImage());

	FileMenu filemenu = new FileMenu("File", this);
	this.menuBar.add(filemenu);

	OptionsMenu optmenu = new OptionsMenu("Options", this, this.prefs);
	this.menuBar.add(optmenu);

	this.menuBar.add(Box.createHorizontalGlue());
	HelpMenu helpMenu = new HelpMenu("Help", this);
	this.menuBar.add(helpMenu);

	setJMenuBar(this.menuBar);

	setPreferredSize(this.windowSize());

	for(int i=0; i<args.length; i++) {
	    if(args[i].length()>0) loadFile(new File(args[i]));
	}
    }

    public void windowClosing(java.awt.event.WindowEvent we) {
	exitApp();
    }

    private void exitApp() {
	this.putWindowSize();
	System.exit(0);
    }

    // Constructor, args
    public ECUxPlot(final String title) {
	this(title, null);
    }

    public static void main(final String[] args) {
	javax.swing.SwingUtilities.invokeLater(new Runnable() {
	    public void run() {
		final ECUxPlot plot = new ECUxPlot("ECUxPlot", args);
		Application app = Application.getApplication();

		if(app!=null) {
		    app.addApplicationListener(new ApplicationAdapter() {
			public void handleOpenFile(ApplicationEvent evt) {
			    String file = evt.getFilename();
			    plot.loadFile(new File(file));
			}
			public void handleQuit(ApplicationEvent evt) {
			    plot.putWindowSize();
			    evt.setHandled(true);
			}
		    });
		}

		plot.pack();
		RefineryUtilities.centerFrameOnScreen(plot);
		plot.setVisible(true);
	    }
	});
    }
}
