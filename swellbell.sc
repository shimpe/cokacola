(
var col0;
var row1, row2;
var row2cols,buttoncol,lowlistviewcol,midlistviewcol,highlistviewcol;
var canvascol,slidegrid;
var absdurationlabel,lowlabel,midlabel,highlabel;
var absduration;

~ui = ();
~ui.columns = []; // enabled, volume_from, volume_to, note_from, note_to, slide, steps, duration, instrument

~ui.absduration = nil;
~ui.loadbutton = nil;
~ui.playstepbutton = nil;
~ui.playandnextbutton = nil;
~ui.playslidebutton = nil;
~ui.registerslidebutton = nil;
~ui.registerstepbutton = nil;
~ui.savebutton = nil;
~ui.lowlistview = nil;
~ui.midlistview = nil;
~ui.highlistview = nil;
~ui.canvas = nil;

~ui.calc_total_duration = { | self |
	var enabled_entries = self.columns.select({ | item, i| item.enabled.value }).postln;
};

~ui.miditoname = ({ arg self, note = 60;
	var midi, notes;
	midi = (note + 0.5).asInteger;
	notes = ["C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"];
	(notes[midi%12] ++ (midi.div(12)-1))
});

~ui.nametomidi = ({ arg self, name = "C4";
	var twelves, ones, octaveIndex, midis;
	midis = Dictionary[($c->0),($d->2),($e->4),($f->5),($g->7),($a->9),($b->11)];
	ones = midis.at(name[0].toLower);
	if( (name[1].isDecDigit),
	{
		octaveIndex = 1;
	}, /*else*/
	{
		octaveIndex = 2;
		if( (name[1] == $#) || (name[1].toLower == $s) || (name[1] == $+), {
			ones = ones + 1;
		},{
			if( (name[1] == $b) || (name[1].toLower == $f) || (name[1] == $-), {
				ones = ones - 1;
			});
		});
	});
	twelves = (name.copyRange(octaveIndex, name.size).asInteger) * 12;
	(twelves + 12 + ones)
});


s.waitForBoot({
	var width = 1900;
	var height = 1000;

	w = Window(bounds: Rect(0, 0, width, height));

	col0 = VLayout.new;
	row1 = HLayout.new;
	row2 = HLayout.new;

	8.do({ |i|
		var col = VLayout.new;
		var cb = CheckBox.new(w, Rect(), "Enabled").action_({ ~ui.canvas.refresh; });

		var volumelabel = StaticText(w, Rect()).string_("Volume").font_(Font("Helvetica-Bold", 18)).backColor_(Color.yellow);
		var volumefrom = StaticText(w, Rect()).string_("from");
		var volumeto = StaticText(w, Rect()).string_("to");
		var volumestart = TextField(w, Rect()).string_("-50");
		var volumeunit = StaticText(w, Rect()).string_("dB");

		var endvolume = TextField(w, Rect()).string_("-6");
		var endvolumeunit = StaticText(w, Rect()).string_("dB");

		var notelabel = StaticText(w, Rect()).string_("Note").font_(Font("Helvetica-Bold", 18)).backColor_(Color.blue.lighten(0.7));
		var notestart = TextField(w, Rect()).string_("C3");
		var notefrom = StaticText(w, Rect()).string_("from");
		var noteto = StaticText(w, Rect()).string_("to");

		var noteend = TextField(w, Rect()).string_("C5");

		var slidelabel = StaticText(w, Rect()).string_("Slide").font_(Font("Helvetica-Bold", 18)).backColor_(Color.cyan.lighten(0.7));
		var slide = TextField(w, Rect()).string_("2");
		var slideunit = StaticText(w, Rect()).string_("halve tones");

		var stepslabel = StaticText(w, Rect()).string_("Steps").font_(Font("Helvetica-Bold", 18)).backColor_(Color.red.lighten(0.7));
		var steps = TextField(w, Rect()).string_("10");

		var durationlabel = StaticText(w, Rect()).string_("Dur.").font_(Font("Helvetica-Bold", 18)).backColor_(Color.green.lighten(0.7));
		var duration = TextField(w, Rect()).string_("5");
		var durationunit = StaticText(w, Rect()).string_("sec");

		var instrumentlabel= StaticText(w, Rect()).string_("Instr.").font_(Font("Helvetica-Bold", 18)).backColor_(Color.magenta.lighten(0.7));
		var instr = PopUpMenu(w).items_(["HellsBells"]);

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
	~ui.loadbutton = Button.new(w, Rect()).string_("Load").states_([["Load",Color.black,Color.gray]]);
	absdurationlabel = StaticText(w, Rect()).string_("Abs. Dur. (sec)");
	~ui.absduration = TextField(w, Rect()).string_("10");
	~ui.playstepbutton = Button.new(w, Rect()).string_("Play step").states_([["Play step",Color.black,Color.gray]]);
	~ui.playandnextbutton = Button.new(w, Rect()).string_("Play&Next").states_([["Play&Next",Color.black,Color.gray]]);
	~ui.playslidebutton = Button.new(w, Rect()).string_("Play slide").states_([["Play slide",Color.black,Color.gray]]);
	~ui.registerslidebutton = Button.new(w, Rect()).string_("Register slide").states_([["Register slide",Color.black,Color.gray]]);
	~ui.registerstepbutton = Button.new(w, Rect()).string_("Register step").states_([["Register step",Color.black,Color.gray]]);
	~ui.savebutton = Button.new(w, Rect()).string_("Save").states_([["Save",Color.black,Color.gray]]);
	buttoncol.add(~ui.loadbutton);
	buttoncol.add(absdurationlabel);
	buttoncol.add(~ui.absduration);
	buttoncol.add(nil);
	buttoncol.add(~ui.playstepbutton);
	buttoncol.add(~ui.playandnextbutton);
	buttoncol.add(nil);
	buttoncol.add(~ui.playslidebutton);
	buttoncol.add(~ui.registerslidebutton);
	buttoncol.add(~ui.registerstepbutton);
	buttoncol.add(~ui.savebutton);

	lowlistviewcol = VLayout.new;
	lowlabel = StaticText(w, Rect()).string_("low").font_(Font("Helvetica-Bold", 18)).backColor_(Color.gray.lighten(0.9)).align_(\center);
	~ui.lowlistview = ListView(w, Rect()).items_(Array.fill(26, {|i| "slide "++i.asString}).put(0, "None"));
	lowlistviewcol.add(lowlabel);
	lowlistviewcol.add(~ui.lowlistview);

	midlistviewcol = VLayout.new;
	midlabel = StaticText(w, Rect()).string_("mid").font_(Font("Helvetica-Bold", 18)).backColor_(Color.gray.lighten(0.9)).align_(\center);
	~ui.midlistview = ListView(w, Rect()).items_(Array.fill(26, {|i| "slide "++i.asString}).put(0, "None"));
	midlistviewcol.add(midlabel);
	midlistviewcol.add(~ui.midlistview);

	highlistviewcol = VLayout.new;
	highlabel = StaticText(w, Rect()).string_("high").font_(Font("Helvetica-Bold", 18)).backColor_(Color.gray.lighten(0.9)).align_(\center);
	~ui.highlistview = ListView(w, Rect()).items_(Array.fill(26, {|i| "slide "++i.asString}).put(0, "None"));
	highlistviewcol.add(highlabel);
	highlistviewcol.add(~ui.highlistview);

	canvascol = VLayout.new;
	~ui.canvas = UserView(w,Rect()).background_(Color.white).minSize = 1400@500;
	slidegrid = GridLayout.rows(
		Array.fill(25,{|i| Button.new(w,Rect()).maxSize_(50@20).string_({(i+1).asString}.value).states_([[{(i+1).asString}.value,Color.black,Color.white]]); }),
		Array.fill(25,{|i| Button.new(w,Rect()).maxSize_(50@20).string_({(i+26).asString}.value) .states_([[{(i+26).asString}.value,Color.black,Color.white]]); })
	);
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
		var bounds = v.bounds;
		var totalduration = ~ui.calc_total_duration(~ui);


		Pen.fillColor = Color.black;
		Pen.stroke;

		w.refresh;
	});

	w.layout = col0;
	w.front;


});

)
