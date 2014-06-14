/*
 * Copyright (C) 2012 Brian Reber
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms are permitted
 * provided that the above copyright notice and this paragraph are
 * duplicated in all such forms and that any documentation,
 * advertising materials, and other materials related to such
 * distribution and use acknowledge that the software was developed
 * by Brian Reber.
 * THIS SOFTWARE IS PROVIDED 'AS IS' AND WITHOUT ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, WITHOUT LIMITATION, THE IMPLIED
 * WARRANTIES OF MERCHANTIBILITY AND FITNESS FOR A PARTICULAR PURPOSE.
 */
package org.reber.agenda.list;

import android.app.ListFragment;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;
import org.reber.agenda.AgendaActivity;
import org.reber.agenda.AndroidCalendar;
import org.reber.agenda.R;
import org.reber.agenda.util.CalendarUtilities;
import org.reber.agenda.util.Constants;

import java.util.*;

/**
 * A fragment that displays the list of upcoming appointments.
 *
 * @author breber
 */
public class AgendaListFragment extends ListFragment {

    private int triggeredWidgetId = -1;

    private CalendarUtilities util;
    private SharedPreferences pref;
    public static int NUM_DAYS_IN_LIST = 7;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(Constants.TAG, "onActivityCreated");

        triggeredWidgetId = -1;
        Bundle b = getActivity().getIntent().getExtras();
        if (b != null) {
            triggeredWidgetId = b.getInt(AgendaActivity.WIDGET_EXTRA, -1);
        }

        pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        util = new CalendarUtilities(getActivity(), pref.getBoolean(Constants.AgendaList.USE_24_HR, false));

        notifyUtilUpdated();
    }

    /**
     * Updates the ListView with the events within the next NUM_DAYS_IN_LIST days
     */
    protected void updateList() {
        try {
            if (triggeredWidgetId == -1) {
                //				util.setSelectedCalendars(util.getSelectedCalendarFromPref(Constants.AgendaList.APP_PREFS));
                util.setSelectedCalendars(util.getSelectedCalendarFromPref(null));
            } else {
                // If we have a valid widget id (this activity was started by clicking on the widget),
                // set the calendars for this run to the calendars from that widget's preferences
                util.setSelectedCalendars(util.getSelectedCalendarFromPref(Constants.Widget.WIDGET_PREFS + triggeredWidgetId));
            }
        } catch (NoSuchElementException e) {
            util.setSelectedCalendars(new HashSet<AndroidCalendar>());
        }

        Collection<Event> events = util.getCalendarData(NUM_DAYS_IN_LIST, true);

        setListAdapter(new CalendarListAdapter(getActivity(), android.R.layout.simple_list_item_1, getListWithDateRows(events), util));

        if (util.getSelectedCalendars().isEmpty()) {
            setEmptyText(getActivity().getResources().getText(R.string.noCalendarsSelected));
        } else if (events.isEmpty()) {
            setEmptyText(getActivity().getResources().getText(R.string.emptyMsg));
        }
    }

    /**
     * Adds Date strings in the list so that they can act as separators in the
     * ListView.
     *
     * @param events
     * @return A list of ListItems containting event data with separator
     *         inserted
     */
    private List<ListItem> getListWithDateRows(Collection<Event> events) {
        LinkedList<ListItem> eventList = new LinkedList<ListItem>(events);
        List<String> dates = new ArrayList<String>();

        ListIterator<ListItem> iter = eventList.listIterator();
        while (iter.hasNext()) {
            ListItem e = iter.next();
            String dateString = CalendarUtilities.getDateString(getActivity(), (Event) e, "E, MMM d");
            if (!dates.contains(dateString)) {
                iter.previous();
                iter.add(new ListSeparator(dateString));
                dates.add(dateString);
            }
        }

        return eventList;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        SharedPreferences pref = getActivity().getSharedPreferences(Constants.AgendaList.APP_PREFS, 0);
        if (pref.getBoolean(Constants.AgendaList.ENABLE_CLICK_EVENT, true)) {
            Object obj = l.getItemAtPosition(position);
            Event ev = null;
            if (obj instanceof Event) {
                ev = (Event) obj;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(CalendarContract.Events.CONTENT_URI + "/" + ev.getId()));
            // Who knows why you need to put the start and end times in the intent,
            // but for some reason you need to for the com.android.calendar app...
            intent.putExtra("beginTime", ev.getStart().getTimeInMillis());
            intent.putExtra("endTime", ev.getEnd().getTimeInMillis());
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(getActivity(), "Unable to open event", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * The util variable might have been updated. We will update our state based on the state
     * of the preferences and then refresh the list
     */
    public void notifyUtilUpdated() {
        util.setUse24Hour(pref.getBoolean(Constants.AgendaList.USE_24_HR, false));
        try {
            util.setSelectedCalendars(util.getSelectedCalendarFromPref(null));
        } catch (NoSuchElementException e) {
            util.setSelectedCalendars(new HashSet<AndroidCalendar>());
        }

        try {
            NUM_DAYS_IN_LIST = Integer.parseInt(pref.getString(Constants.AgendaList.NUM_DAYS, "7"));
        } catch (NumberFormatException e) {
            NUM_DAYS_IN_LIST = 7;
        }

        updateList();
    }

}
