(
// groep 1
// CoKa CoLa - COntemporary KAgel COmposition LAb
var o = Server.local.options;
var supported_instruments = ["hellsbells"];
var no_of_cols = 8;
var col0;
var row0, row1, row2;
var row2cols,buttoncol,lowlistviewcol,midlistviewcol,highlistviewcol;
var canvascol,slidegrid;
var absdurationlabel,lowlabel,midlabel,highlabel;
var absduration;
var arr_helper1, arr_helper2;
var current_columns_for_pattern = ();

var volumedefs = [[\ffff : 0], [\fff : -5], [\ff : -10], [\f : -15], [\mf : -20], [\mp : -25], [\p : -30], [\pp : -35], [\ppp : -40], [\pppp : -45]];

o.memSize = 8192*30;

~active_step_button = 0;
~slidecollection = (); // mapping from ( low/mid/high, slide index ) => slidemodel (column *values*: enabled, volume_from, volume_to, note_from, note_to, slide, steps, duration, instrument)
~slidecollection.data = ();
~slidecollection.data.volumes = ();
~slidecollection.make_slidekey = ({ |self, lowmidhigh, slideno |
	(lowmidhigh ++ "_" ++ slideno).asString;
});

~slidecollection.from_ui = ({ |self, ui, slidekey |
	var columns = ();
	var vdefs = [];
	no_of_cols.do({ | i |
		var columnvalues = ();
		columnvalues.enabled = ui[\columns][i].enabled.value;
		columnvalues.volume_from = ui[\columns][i].volume_from.value;
		columnvalues.volume_to = ui[\columns][i].volume_to.value;
		columnvalues.note_from = ui[\columns][i].note_from.value;
		columnvalues.note_to = ui[\columns][i].note_to.value;
		columnvalues.slide = ui[\columns][i].slide.value;
		columnvalues.steps = ui[\columns][i].steps.value;
		columnvalues.duration = ui[\columns][i].duration.value;
		columnvalues.instrument = ui[\columns][i].instrument.value;
		columns[i.asSymbol] = columnvalues;
	});
	columns.absduration = ui[\absduration].value;

	self[\data][slidekey.asSymbol] = columns;

	vdefs = ();
	volumedefs.do({ | entry, i |
		var key = entry[0];
		vdefs[key] = ui[\volumes][key].value;
	});
	self[\data][\volumes] = vdefs;
});

~slidecollection.to_ui = ({ | self, ui, slidekey |

	if ((self[\data][\volumes].notNil), {
		self[\data][\volumes].keysValuesDo({
			| key, val |
			ui[\volumes][key].value_(val);
		});
	});

	if ((self[\data][slidekey.asSymbol].notNil),{
		var columns = self[\data][slidekey.asSymbol];
		ui[\absduration].value_(columns.absduration);
		no_of_cols.do({ | i |
			ui[\columns][i].enabled.value_(columns[i.asSymbol].enabled);
			ui[\columns][i].volume_from.value_(columns[i.asSymbol].volume_from);
			ui[\columns][i].volume_to.value_(columns[i.asSymbol].volume_to);
			ui[\columns][i].note_from.value_(columns[i.asSymbol].note_from);
			ui[\columns][i].note_to.value_(columns[i.asSymbol].note_to);
			ui[\columns][i].slide.value_(columns[i.asSymbol].slide);
			ui[\columns][i].steps.value_(columns[i.asSymbol].steps);
			ui[\columns][i].duration.value_(columns[i.asSymbol].duration);
			ui[\columns][i].instrument.value_(columns[i.asSymbol].instrument);
		});

		ui[\canvas].refresh;
	});
});

~slidecollection.on_save = ({ | self, name |
	d = self[\data];
	d.writeArchive(Document.current.dir +/+ name);
});

~slidecollection.on_load = ({ |self, name |
	~slidecollection.data = Object.readArchive(Document.current.dir +/+ name);
});

~sequencemodel = (); // mapping from ( step idx ) => ( low/mid/high, slide index)
~sequencemodel.data = ();
~sequencemodel.on_save = ({ |self, name |
	var d = self[\data];
	d.writeArchive(Document.current.dir +/+ name);
});
~sequencemodel.on_load = ({ | self, name|
	~sequencemodel.data = Object.readArchive(Document.current.dir +/+ name);
});

~ui = ();
~ui.volumes = ();
~ui.columns = []; // column *controls* enabled, volume_from, volume_to, note_from, note_to, slide, steps, duration, instrument
~ui.stepbuttons = []; // 0-49
~ui.absduration = nil;
~ui.loadbutton = nil;
~ui.playstepbutton = nil;
~ui.playandnextbutton = nil;
~ui.playslidebutton = nil;
~ui.registerslidebutton = nil;
~ui.unregisterslidebutton = nil;
~ui.registerstepbutton = nil;
~ui.unregisterstepbutton = nil;
~ui.savebutton = nil;
~ui.lowlistview = nil;
~ui.midlistview = nil;
~ui.highlistview = nil;
~ui.canvas = nil;

~ui.on_volume_update = { | self, slidecollection, key, value |
	slidecollection[\data][\volumes][key] = value;
};

~ui.calc_enabled_entries = { |self|
	self.columns.select({ | item, i| item.enabled.value });
};

~ui.calc_total_duration = { | self, enabled_entries |
	enabled_entries.value(self).sum({|item, i| item.duration.value.asFloat });
};

~ui.calc_highest_midi_note = { |self, enabled_entries|
	var item_with_highest_note = enabled_entries.value(self).maxItem({ |item, i|
		[self[\nametomidi].value(self, item.note_from.value.asString),
			self[\nametomidi].value(self, item.note_to.value.asString)].maxItem
	});
	if ((item_with_highest_note.notNil),{
        item_with_highest_note;
		[self[\nametomidi].value(self, item_with_highest_note.note_to.value.asString), self[\nametomidi].value(self, item_with_highest_note.note_from.value.asString)].maxItem;
	},
	/* else */ {
		0;
	});
};

~ui.calc_lowest_midi_note = { |self, enabled_entries|
	var item_with_lowest_note = enabled_entries.value(self).minItem({ |item, i|
		[self[\nametomidi].value(self, item.note_from.value.asString),
			self[\nametomidi].value(self, item.note_to.value.asString)].minItem
	});
	if ((item_with_lowest_note.notNil),{
        item_with_lowest_note;
		[self[\nametomidi].value(self, item_with_lowest_note.note_to.value.asString), self[\nametomidi].value(self, item_with_lowest_note.note_from.value.asString)].minItem;
	},
	/* else */ {
		0;
	});
};

~ui.miditoname = ({ |self, note = 60|
	var midi, notes;
	midi = (note + 0.5).asInteger;
	notes = ["C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"];
	(notes[midi%12] ++ (midi.div(12)-1))
});

~ui.nametomidi = ({ | self, name |
	var twelves, ones, octaveIndex, midis;
	if ((name.isNil or: name == "0"),{
		0;
	}, /*else*/{
		midis = Dictionary[($c->0),($d->2),($e->4),($f->5),($g->7),($a->9),($b->11)];
		ones = midis.at(name[0].toLower);
		if( (name[1].isDecDigit),{
			octaveIndex = 1;
		}, /*else*/{
			octaveIndex = 2;
			if( (name[1] == $#) || (name[1].toLower == $s) || (name[1] == $+),{
				ones = ones + 1;
			}, /*else*/{
				if( (name[1] == $b) || (name[1].toLower == $f) || (name[1] == $-),{
					ones = ones - 1;
				});
			});
		});
		twelves = (name.copyRange(octaveIndex, name.size).asInteger) * 12;
		(twelves + 12 + ones)
	});
});

~ui.duration_to_x = ({ | self, canvaswidth, margin, total_dur, dur |
	dur.linlin(0, total_dur, 0, (canvaswidth-(2*margin))) + margin;
});

~ui.note_to_y = ({ | self, canvasheight, margin, min_note, max_note, note |
	note.linlin(min_note, max_note, (canvasheight-(2*margin)), 0) + margin;
});

~ui.on_step_button = ({ | self, idx, sequencemodel, slidecollection |
	self[\stepbuttons].do({
		| item, i |
		if ((i != idx), {
			if ((sequencemodel[\data][i.asSymbol].notNil), {
				item.value_(3);
			}, /* else */
			{
				item.value_(0);
			});
		}, /* else */
		{
			item.value_(1);
			~active_step_button = idx;
			self[\update_for_selected_step].value(self, idx, sequencemodel, slidecollection)
		});
	});
});

~ui.on_register_active_step_button = ({ |self, slidecollection, sequencemodel |
	sequencemodel[\data][~active_step_button.asSymbol] = self[\get_active_slidekey].value(self, slidecollection);
});

~ui.on_unregister_active_step_button = ({ |self, slidecollection, sequencemodel |
	~sequencemodel[\data].removeAt(~active_step_button.asSymbol);
});

~ui.update_for_selected_step = ({ | self, idx, sequencemodel, slidecollection |
	if ((sequencemodel[\data][~active_step_button.asSymbol].notNil), {
		self[\set_active_slidekey].value(self, sequencemodel[\data][~active_step_button.asSymbol]);
	});
});

~ui.get_active_slidekey = ({ | self, slidecollection |
	var lowmidhigh = "";
	var slideno = 0;

	if ((self.lowlistview.value != 0), {
		lowmidhigh = "low";
		slideno = self.lowlistview.value;
	}, /* else */
	{
		if ((self.midlistview.value != 0), {
			lowmidhigh = "mid";
			slideno = self.midlistview.value;
		}, /* else */
		{
			lowmidhigh = "high";
			slideno = self.highlistview.value;
		});
	});

	slidecollection[\make_slidekey].value(slidecollection, lowmidhigh, slideno);
});

~ui.set_active_slidekey = ({ | self, slidekey |
	var splitted = slidekey.split($_);
	var lowmidhigh = splitted[0];
	var number = splitted[1].asInteger;
	if ((lowmidhigh == "low"), {
		self.lowlistview.valueAction_(number);
	}, /* else */{
		if ((lowmidhigh == "mid"), {
			self.midlistview.valueAction_(number);
		}, /* else */{
			self.highlistview.valueAction_(number);
		});
	});
});

~ui.update_listview_colors = ({| self, slidecollection |
	self.lowlistview.colors_({
		Array.fill(26, {|i|
			var slidekey = slidecollection[\make_slidekey].value(slidecollection, "low", i);
			if ((slidecollection[\data][slidekey.asSymbol].notNil), {
				Color.green;
			}, /* else */
			{
				Color.white;
			});

		});
	}.value);
	self.midlistview.colors_({
		Array.fill(26, {|i|
			var slidekey = slidecollection[\make_slidekey].value(slidecollection, "mid", i);
			if ((slidecollection[\data][slidekey.asSymbol].notNil), {
				Color.green;
			}, /* else */
			{
				Color.white;
			});

		});
	}.value);
	self.highlistview.colors_({
		Array.fill(26, {|i|
			var slidekey = slidecollection[\make_slidekey].value(slidecollection, "high", i);
			if ((slidecollection[\data][slidekey.asSymbol].notNil), {
				Color.green;
			}, /* else */
			{
				Color.white;
			});

		});
	}.value);
});

~ui.on_save = ({ | self, slidecollection, sequencemodel |

	var d;
	var vdefs = ();
	volumedefs.do({ | entry, i |
		var key = entry[0];
		vdefs[key] = self[\volumes][key].value;
	});
	slidecollection[\data][\volumes] = vdefs;

	slidecollection[\on_save].value(~slidecollection, "slide_def.scpreset");
	sequencemodel[\on_save].value(~sequencemodel, "seq_def.scpreset");
});

~ui.on_load = ({ | self, slidecollection, sequencemodel |
	~slidecollection = slidecollection[\on_load].value(~slidecollection, "slide_def.scpreset");
	~sequencemodel = sequencemodel[\on_load].value(~sequencemodel, "seq_def.scpreset");
	slidecollection[\to_ui].value(slidecollection, ~ui, "low_0");
	self[\set_active_slidekey].value(self, "low_0");
	self[\update_listview_colors].value(self, slidecollection);
	self[\on_step_button].value(self, 0, ~sequencemodel, ~slidecollection);
});

~ui.on_play_slide = ({| self |
	var enabled_entries= ~ui[\calc_enabled_entries].value(~ui);
	var total_duration = ~ui[\calc_total_duration].value(~ui, enabled_entries);
	var enabledkeys = [];
	no_of_cols.do({ | i |
		var steps = ~ui[\columns][i].steps.value.asInteger;
		var tspan = ((~ui[\columns][i].duration.value.asFloat)/total_duration)*(~ui[\absduration].value.asFloat);
		if ((steps == 0), {
			"REST".postln;
			if ((~ui[\columns][i].enabled.value),{
				Pdef(("p"++i).asSymbol).quant = 0;
				Pdef(("p"++i).asSymbol).fadeTime = 0.5;
				Pdef(("p"++i).asSymbol,
					Pbind(
						\instrument, \default,
						\type, \rest,
						\dur, Pseq([tspan/50],50);
					);
				);
				enabledkeys = enabledkeys.add(("p"++i).asSymbol);
			});
		}, /* else */{
			var instrname = supported_instruments[~ui[\columns][i].instrument.value];
			var f1a = (~ui[\nametomidi].value(~ui, ~ui[\columns][i].note_from.value));
			var f1b = (~ui[\nametomidi].value(~ui, ~ui[\columns][i].note_to.value));
			var f2a = f1a+(~ui[\columns][i].slide.value.asFloat);
			var f2b = f1b+(~ui[\columns][i].slide.value.asFloat);
			var vola = ~ui[\columns][i].volume_from.value.asFloat.dbamp;
			var volb = ~ui[\columns][i].volume_to.value.asFloat.dbamp;

			if ((steps == 1), {
				"ONESTEP".postln;
				if ((~ui[\columns][i].enabled.value),{
					Pdef(("p"++i).asSymbol).quant = 0;
					Pdef(("p"++i).asSymbol).fadeTime = 0.5;
					Pdef(("p"++i).asSymbol,
						Pbind(
							\instrument, instrname.asSymbol,
							\freqstart, Pseq([f1a], 1),
							\freqend, Pseq([f1b], 1),
							\startvol, Pseq([vola], 1),
							\endvol, Pseq([volb], 1),
							\timespan, Pseq([tspan], 1),
							\amp, Pseq([(vola+volb)/2], 1),
							\dur, Pseq([tspan],1)
						);
					);
					enabledkeys = enabledkeys.add(("p"++i).asSymbol);
				});
			}, /* else */
			{
				var f1atob = series(f1a, (f1a+((f1b-f1a)/steps)), f1b).midicps;
				var f1atobsize = f1atob.size;
				var f2atob = series(f2a, (f2a+((f2b-f2a)/steps)), f2b).midicps;
				var dur = tspan/f1atobsize;
				steps = steps - 1;
				if ((~ui[\columns][i].enabled.value),{
					"MULTISTEP".postln;
					Pdef(("p"++i).asSymbol).quant = 0;
					Pdef(("p"++i).asSymbol).fadeTime = 0.5;
					Pdef(("p"++i).asSymbol,
						Pbind(
							\instrument, instrname.asSymbol,
							\freqstart, Pseq(f1atob, inf),
							\freqend, Pseq(f2atob, inf),
							\startvol, Pseq([vola], inf),
							\endvol, Pseq([volb], inf),
							\timespan, Pseq([tspan], inf),
							\amp, Pseries(vola, ((volb - vola)/f1atobsize), f1atobsize),
							\dur, Pseq([dur], inf)
						);
					);
					enabledkeys = enabledkeys.add(("p"++i).asSymbol);
				});

			});
		});
	});

	Pdef(\masterpattern, Psym(Pseq(enabledkeys, 1))).play;

});

~ui.on_play_step = ({ | self |
	~ui[\stepbuttons]
});

s.waitForBoot({
	var width = 1900;
	var height = 1000;

	SynthDef(\hellsbells, {|out=0, pan=0, freqstart=440, freqend=880, amp=0.1, dur=2|
		var t = [1, 3.2, 6.23, 6.27, 9.92, 14.15]; // partials
		var a = amp*t.collect({ |i,idx| 1*(0.99*idx) }); // amplitude per partial
		var u = t.inject([],
			{   | collection_so_far, newelement |
				var csize = collection_so_far.size;
				var temp = [];
				(csize+3).do({
					|item, idx|
					temp = temp ++ [newelement + idx - ((csize+2)/2)];
				});
				collection_so_far = collection_so_far ++ temp;
		});
		var v = a.inject([],
			{   | collection_so_far, newelement |
				var csize = collection_so_far.size;
				var temp = [];
				(csize+3).do({
					|item, idx|
					temp = temp ++ [ 0.95**((idx - ((csize+2)/2)).abs)];
				});
				collection_so_far = collection_so_far ++ temp;
		});

		var signal = (10*amp/u.size)*EnvGen.kr(Env.perc(0.1,dur,1), doneAction:Done.freeSelf)*DynKlank.ar(`[u*XLine.kr(freqstart, freqend, dur), v, v], BrownNoise.ar([0.007,0.007]));
		Out.ar(out, Pan2.ar(signal));
	}).add;

	s.sync;

	w = Window(bounds: Rect(0, 0, width, height));

	col0 = VLayout.new;

	row0 = HLayout.new;
	volumedefs.do({ | entry, idx |
		var label = StaticText(w, Rect()).string_(entry[0].asString);
		var tf = TextField(w, Rect()).string_(entry[1].asString).action_({ ~ui[\on_volume_update].value(~ui, ~slidecollection, entry[0], entry[1]); });
		row0.add(label);
		~ui[\volumes][entry[0]] = tf;
		row0.add(tf);
	});
	col0.add(row0);

	row1 = HLayout.new;
	row2 = HLayout.new;

	no_of_cols.do({ |i|
		var col = VLayout.new;
		var cb = CheckBox.new(w, Rect(), "Enabled").action_({ ~ui.canvas.refresh; });

		var volumelabel = StaticText(w, Rect()).string_("Volume").font_(Font("Helvetica-Bold", 18)).backColor_(Color.yellow);
		var volumefrom = StaticText(w, Rect()).string_("from");
		var volumeto = StaticText(w, Rect()).string_("to");
		var volumestart = TextField(w, Rect()).string_("-50").action_({ ~ui.canvas.refresh; });
		var volumeunit = StaticText(w, Rect()).string_("dB");

		var endvolume = TextField(w, Rect()).string_("-6").action_({ ~ui.canvas.refresh; });
		var endvolumeunit = StaticText(w, Rect()).string_("dB");

		var notelabel = StaticText(w, Rect()).string_("Note").font_(Font("Helvetica-Bold", 18)).backColor_(Color.blue.lighten(0.7));
		var notestart = TextField(w, Rect()).string_("C3").action_({ ~ui.canvas.refresh; });
		var notefrom = StaticText(w, Rect()).string_("from");
		var noteto = StaticText(w, Rect()).string_("to");

		var noteend = TextField(w, Rect()).string_("C5").action_({ ~ui.canvas.refresh; });

		var slidelabel = StaticText(w, Rect()).string_("Slide").font_(Font("Helvetica-Bold", 18)).backColor_(Color.cyan.lighten(0.7));
		var slide = TextField(w, Rect()).string_("2").action_({ ~ui.canvas.refresh; });
		var slideunit = StaticText(w, Rect()).string_("halve tones");

		var stepslabel = StaticText(w, Rect()).string_("Steps").font_(Font("Helvetica-Bold", 18)).backColor_(Color.red.lighten(0.7));
		var steps = TextField(w, Rect()).string_("10").action_({ ~ui.canvas.refresh; });

		var durationlabel = StaticText(w, Rect()).string_("Dur.").font_(Font("Helvetica-Bold", 18)).backColor_(Color.green.lighten(0.7));
		var duration = TextField(w, Rect()).string_("5").action_({ ~ui.canvas.refresh; });
		var durationunit = StaticText(w, Rect()).string_("sec");

		var instrumentlabel= StaticText(w, Rect()).string_("Instr.").font_(Font("Helvetica-Bold", 18)).backColor_(Color.magenta.lighten(0.7));
		var instr = PopUpMenu(w).items_(supported_instruments);

		var paramgrid = GridLayout.rows(
			[volumelabel, nil, nil],
			[volumefrom, volumestart, volumeunit],
			[volumeto, endvolume, endvolumeunit],
			[notelabel, nil, nil],
			[notefrom, notestart, nil],
			[noteto, noteend, nil],
			[slidelabel, slide, slideunit],
			[stepslabel, steps, nil],
			[durationlabel, duration, durationunit],
			[instrumentlabel, instr, nil],
		);

		var column = ();
		column.enabled = cb;
		column.volume_from = volumestart;
		column.volume_to = endvolume;
		column.note_from = notestart;
		column.note_to = noteend;
		column.slide = slide;
		column.steps = steps;
		column.duration = duration;
		column.instrument = instr;
		~ui.columns = ~ui.columns.add(column);

		col.add(cb);
		col.add(paramgrid);
		col.add(nil);

		row1.add(col);
	});
	col0.add(row1);

	row2cols = HLayout.new;

	buttoncol = VLayout.new;
	~ui.loadbutton = Button.new(w, Rect()).string_("Load").states_([["Load",Color.black,Color.gray]]).action_({ ~ui[\on_load].value(~ui, ~slidecollection, ~sequencemodel)});
	absdurationlabel = StaticText(w, Rect()).string_("Abs. Dur. (sec)");
	~ui.absduration = TextField(w, Rect()).string_("10");
	~ui.playstepbutton = Button.new(w, Rect()).string_("Play step").states_([["Play step",Color.black,Color.gray]]).action_({ ~ui[\on_play_step].value(~ui);});
	~ui.playandnextbutton = Button.new(w, Rect()).string_("Play&Next").states_([["Play&Next",Color.black,Color.gray]]);
	~ui.playslidebutton = Button.new(w, Rect()).string_("Play slide").states_([["Play slide",Color.black,Color.gray]]).action_({ ~ui[\on_play_slide].value(~ui); });
	~ui.registerslidebutton = Button.new(w, Rect())
	   .string_("Register slide")
	   .states_([["Register slide",Color.black,Color.green]])
	   .action_({ | b |
		~slidecollection[\from_ui].value(~slidecollection, ~ui, ~ui[\get_active_slidekey].value(~ui, ~slidecollection));
		~ui[\update_listview_colors].value(~ui, ~slidecollection);
	});
	~ui.unregisterslidebutton = Button.new(w, Rect())
	   .string_("Unregister slide")
	   .states_([["Unregister slide",Color.black,Color.red]])
	   .action_({ | b |
		~slidecollection[\data].removeAt(~ui[\get_active_slidekey].value(~ui, ~slidecollection).asSymbol);
		~ui[\update_listview_colors].value(~ui, ~slidecollection);
	});
	~ui.registerstepbutton = Button.new(w, Rect()).string_("Register step").states_([["Register step",Color.black,Color.green]]).action_({~ui[\on_register_active_step_button].value(~ui, ~slidecollection, ~sequencemodel)});
	~ui.unregisterstepbutton = Button.new(w, Rect()).string_("Unregister step").states_([["Unregister step",Color.black,Color.red]]).action_({~ui[\on_unregister_active_step_button].value(~ui, ~slidecollection, ~sequencemodel)});

	~ui.savebutton = Button.new(w, Rect()).string_("Save").states_([["Save",Color.black,Color.gray]]).action_({ ~ui[\on_save].value(~ui, ~slidecollection, ~sequencemodel); });
	buttoncol.add(~ui.loadbutton);
	buttoncol.add(absdurationlabel);
	buttoncol.add(~ui.absduration);
	buttoncol.add(nil);
	buttoncol.add(~ui.playstepbutton);
	buttoncol.add(~ui.playandnextbutton);
	buttoncol.add(nil);
	buttoncol.add(~ui.playslidebutton);
	buttoncol.add(~ui.registerslidebutton);
	buttoncol.add(~ui.unregisterslidebutton);
	buttoncol.add(~ui.registerstepbutton);
	buttoncol.add(~ui.unregisterstepbutton);
	buttoncol.add(~ui.savebutton);

	lowlistviewcol = VLayout.new;
	lowlabel = StaticText(w, Rect()).string_("low").font_(Font("Helvetica-Bold", 18)).backColor_(Color.gray.lighten(0.9)).align_(\center);

	~ui.lowlistview = ListView(w, Rect())
	   .items_(Array.fill(26, {|i| "slide "++i.asString})
	   .put(0, "None"))
	   .action_({
		| sel |
		var slidekey;
		var slide_number = sel.value;
		~ui.midlistview.value_(0);
		~ui.highlistview.value_(0);
		slidekey = ~ui[\get_active_slidekey].value(~ui, ~slidecollection);
		~slidecollection[\to_ui].value(~slidecollection, ~ui, slidekey);
	});
	lowlistviewcol.add(lowlabel);
	lowlistviewcol.add(~ui.lowlistview);

	midlistviewcol = VLayout.new;
	midlabel = StaticText(w, Rect()).string_("mid").font_(Font("Helvetica-Bold", 18)).backColor_(Color.gray.lighten(0.9)).align_(\center);
	~ui.midlistview = ListView(w, Rect())
	   .items_(Array.fill(26, {|i| "slide "++i.asString})
	   .put(0, "None"))
	   .action_({
		| sel |
		var slidekey;
		var slide_number = sel.value;
		~ui.lowlistview.value_(0);
		~ui.highlistview.value_(0);
		slidekey = ~ui[\get_active_slidekey].value(~ui, ~slidecollection);
		~slidecollection[\to_ui].value(~slidecollection, ~ui, slidekey);
	});
	midlistviewcol.add(midlabel);
	midlistviewcol.add(~ui.midlistview);

	highlistviewcol = VLayout.new;
	highlabel = StaticText(w, Rect()).string_("high").font_(Font("Helvetica-Bold", 18)).backColor_(Color.gray.lighten(0.9)).align_(\center);
	~ui.highlistview = ListView(w, Rect())
	   .items_(Array.fill(26, {|i| "slide "++i.asString})
	   .put(0, "None"))
	   .action_({
		| sel |
		var slidekey;
		var slide_number = sel.value;
		~ui.lowlistview.value_(0);
		~ui.midlistview.value_(0);
		slidekey = ~ui[\get_active_slidekey].value(~ui, ~slidecollection);
		~slidecollection[\to_ui].value(~slidecollection, ~ui, slidekey);
	});
	highlistviewcol.add(highlabel);
	highlistviewcol.add(~ui.highlistview);

	canvascol = VLayout.new;
	~ui.canvas = UserView(w,Rect()).background_(Color.white).minSize = 1400@500;

	arr_helper1 = Array.fill(25,{|i| Button.new(w,Rect()).maxSize_(50@20).string_({(i+1).asString}.value).states_([[{(i+1).asString}.value,Color.black,Color.white],[{(i+1).asString}.value,Color.black,Color.blue.lighten(0.5)],[{(i+1).asString}.value,Color.black,Color.green.lighten(0.5)]]).action_({ |button| ~ui[\on_step_button].value(~ui, i, ~sequencemodel, ~slidecollection); }); });
	~ui[\stepbuttons] = [];
	~ui[\stepbuttons] = ~ui[\stepbuttons].addAll(arr_helper1);
	arr_helper2 = Array.fill(25,{|i| Button.new(w,Rect()).maxSize_(50@20).string_({(i+26).asString}.value) .states_([[{(i+26).asString}.value,Color.black,Color.white],[{(i+26).asString}.value,Color.black,Color.blue.lighten(0.5)],[{(i+1).asString}.value,Color.black,Color.green.lighten(0.5)]]).action_({| button | ~ui[\on_step_button].value(~ui, i+25, ~sequencemodel, ~slidecollection)}); });
	~ui[\stepbuttons] = ~ui[\stepbuttons].addAll(arr_helper2);
	arr_helper1[0].valueAction_(1);
	slidegrid = GridLayout.rows(arr_helper1, arr_helper2);
	canvascol.add(~ui.canvas);
	canvascol.add(slidegrid);

	row2cols.add(buttoncol);
	row2cols.add(lowlistviewcol);
	row2cols.add(midlistviewcol);
	row2cols.add(highlistviewcol);
	row2cols.add(canvascol);

	row2.add(row2cols);
	col0.add(row2);

	~ui.canvas.drawFunc_({ |v|
		var margin = 10;
		var bounds = v.bounds;
		var canvaswidth = v.bounds.width - (2*margin);
		var canvasheight = v.bounds.height - (2*margin);
		var enabled_entries = ~ui[\calc_enabled_entries].value(~ui);
		var totalduration = ~ui[\calc_total_duration].value(~ui, enabled_entries);
		var highestnote = ~ui[\nametomidi].value(~ui, "B9"); // no autozoom in y
		var lowestnote = ~ui[\nametomidi].value(~ui, "C0");  // no autozoom in y

        //var highestnote = ~ui[\calc_highest_midi_note].value(~ui, enabled_entries);   // use these lines to autozoom in y
		//var lowestnote = ~ui[\calc_lowest_midi_note].value(~ui, enabled_entries);     // use these lines to autozoom in y

		var inc_duration = 0;

		Pen.fillColor = Color.black;
		enabled_entries.do({ | item, i |
			var beginvolume = item.volume_from.value.asFloat;
			var endvolume = item.volume_to.value.asFloat;
			var dvol = ((beginvolume - endvolume).abs);
			var fromnote = ~ui[\nametomidi].value(~ui,item.note_from.value.asString);
			var tonote = ~ui[\nametomidi].value(~ui,item.note_to.value.asString);
			var x1 = ~ui[\duration_to_x].value(~ui, canvaswidth, margin, totalduration, inc_duration);
			var y1 = ~ui[\note_to_y].value(~ui, canvasheight, margin, lowestnote, highestnote, fromnote);
			var x2 = ~ui[\duration_to_x].value(~ui, canvaswidth, margin, totalduration, (item.duration.value.asFloat + inc_duration));
			var y2 = ~ui[\note_to_y].value(~ui, canvasheight, margin, lowestnote, highestnote, tonote);
			var fromstring = (item.note_from.value.asString++";"++item.volume_from.value.asString++"dB");
			var tostring = (item.note_to.value.asString++";"++item.volume_to.value.asString++"dB");
			var tostring_width = tostring.bounds(Font("Courier",20)).width;
			var steps = item.steps.value;
			var steps_string = (steps.asString ++ " steps;" ++ item.duration.value.asString ++ " dur");

			if ((beginvolume < endvolume),{
				if (((fromnote != 0) && (tonote != 0) && (steps.asInteger!=0)), {
					Pen.line(x1@y1, x2@(y2+(dvol/2)));
					Pen.line(x1@y1, x2@(y2-(dvol/2)));
					Pen.stroke;
				}, /* else */ {
					Pen.use {
						Pen.fillColor = Color.black.lighten(0.9);
						Pen.addRect(Rect.fromPoints(x1@y1, x2@y2));
						Pen.fill;
					}
				})
			},/* else */
			{
				if((beginvolume > endvolume),{
					if (((fromnote != 0) && (tonote != 0) && (steps.asInteger!=0)), {
						Pen.line(x1@(y1+(dvol/2)), x2@y2);
						Pen.line(x1@(y1-(dvol/2)), x2@y2);
						Pen.stroke;
					}, /* else */ {
						Pen.use {
							Pen.fillColor = Color.black.lighten(0.9);
							Pen.addRect(Rect.fromPoints(x1@y1, x2@y2));
							Pen.fill;
						}
					})
				},/* else */{
					if (((fromnote != 0) && (tonote != 0) && (steps.asInteger != 0)), {
						Pen.line(x1@y1, x2@y2);
						Pen.stroke;
					}, /* else */ {
						Pen.use {
							Pen.fillColor = Color.black.lighten(0.9);
							Pen.addRect(Rect.fromPoints(x1@y1, x2@y2));
							Pen.fill;
						}
					})
				})
			});

			if ( ((fromnote != 0) && (tonote != 0) && (steps.asInteger != 0)), {
				fromstring.drawAtPoint(
					x1@y1,
					Font("Courier",20),
					Color.blue(0.3,0.5));
				tostring.drawAtPoint(
					(x2-tostring_width)@y2,
					Font("Courier",20),
					Color.blue(0.3,0.5));
				steps_string.drawAtPoint(
					((x1+x2)/2)@((y1+y2-(dvol/2))/2),
					Font("Courier",20),
					Color.blue(0.3,0.5));
				Pen.stroke;
			});

			inc_duration = inc_duration + item.duration.value.asFloat;
		});

		w.refresh;
	});

	w.layout = col0;
	w.front;
});

)
