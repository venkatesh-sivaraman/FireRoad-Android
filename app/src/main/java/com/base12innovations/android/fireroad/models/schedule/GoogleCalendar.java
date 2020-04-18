package com.base12innovations.android.fireroad.models.schedule;

import android.app.Activity;
import android.content.Context;

import com.base12innovations.android.fireroad.MainActivity;
import com.base12innovations.android.fireroad.models.course.Course;
import com.base12innovations.android.fireroad.models.doc.ScheduleDocument;
import com.base12innovations.android.fireroad.utils.TaskDispatcher;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GoogleCalendar {

    public static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = {
            CalendarScopes.CALENDAR,
            CalendarScopes.CALENDAR_EVENTS,
    };

    public static GoogleAccountCredential credentials;
    private static final JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
    private static HttpTransport httpTransport;
    private static Activity mainActivity;
    private static String TIME_ZONE = "America/New_York";



    public static void initialize(Activity activity) {
        mainActivity = activity;
        credentials = GoogleAccountCredential.usingOAuth2(activity.getApplicationContext(), Arrays.asList(SCOPES)).setBackOff(new ExponentialBackOff()).setSelectedAccountName(mainActivity.getPreferences(Context.MODE_PRIVATE).getString(PREF_ACCOUNT_NAME, null));
        httpTransport = AndroidHttp.newCompatibleTransport();
    }

    private static EventDateTime getEventDateTime(java.util.Calendar day, Course.ScheduleTime time){
        java.util.Calendar calendar = (java.util.Calendar)day.clone();
        calendar.set(Calendar.AM_PM, (time.PM)? Calendar.PM : Calendar.AM);
        if(time.hour == 12){
            calendar.set(Calendar.HOUR,0);
        }else {
            calendar.set(Calendar.HOUR, time.hour);
        }
        calendar.set(Calendar.MINUTE, time.minute);
        return new EventDateTime().setDateTime(new DateTime(calendar.getTime())).setTimeZone(TIME_ZONE);
    }

    private static Event createEvent(String eventSummary, java.util.Calendar day, Course.ScheduleTime startTime, Course.ScheduleTime endTime, String endRecDate){
        return new Event().setSummary(eventSummary).setStart(getEventDateTime(day,startTime)).setEnd(getEventDateTime(day,endTime)).setRecurrence(Collections.singletonList("RRULE:FREQ=WEEKLY;UNTIL=" + endRecDate));
    }

    private static List<Event> getScheduleEvents(ScheduleDocument scheduleDocument, java.util.Calendar startCalendar, java.util.Calendar endCalendar){
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US);
        Map<String,String> colorIDforCourse = new HashMap<>();
        int counter = 0;
        for (Course c : scheduleDocument.getAllCourses()) {
            colorIDforCourse.put(c.getSubjectID(),String.valueOf(++counter));
        }
        List<Event> events = new ArrayList<>();
        int startDay;
        switch (startCalendar.get(Calendar.DAY_OF_WEEK)){
            case Calendar.SUNDAY: startDay = Course.ScheduleDay.SUN; break;
            case Calendar.MONDAY: startDay = Course.ScheduleDay.MON; break;
            case Calendar.TUESDAY: startDay = Course.ScheduleDay.TUES; break;
            case Calendar.WEDNESDAY: startDay = Course.ScheduleDay.WED; break;
            case Calendar.THURSDAY: startDay = Course.ScheduleDay.THURS; break;
            case Calendar.FRIDAY: startDay = Course.ScheduleDay.FRI; break;
            default: startDay = Course.ScheduleDay.SAT; break;
        }
        int startDayIndex = Course.ScheduleDay.indexOf(startDay);
        if(startDayIndex != 0)
            startCalendar.add(Calendar.DAY_OF_MONTH,7 - startDayIndex);
        for(int day : Course.ScheduleDay.ordering){
            if(startDayIndex != 0 && startDay == day){
                startCalendar.add(Calendar.DAY_OF_MONTH, -6);
            }else if(Course.ScheduleDay.indexOf(day)!= 0){
                startCalendar.add(Calendar.DAY_OF_MONTH,1);
            }
            List<ScheduleConfiguration.ChronologicalElement> chronologicalElements = scheduleDocument.selectedSchedule.chronologicalItemsForDay(day);
            for(ScheduleConfiguration.ChronologicalElement chronologicalElement : chronologicalElements){
                events.add(createEvent(chronologicalElement.course.getSubjectID() + " " + chronologicalElement.type,
                        startCalendar, chronologicalElement.item.startTime, chronologicalElement.item.endTime, dateFormat.format(endCalendar.getTime())).setColorId(colorIDforCourse.get(chronologicalElement.course.getSubjectID())));
            }
        }
        return events;
    }

    public static void addToCalendar(ScheduleDocument scheduleDocument, java.util.Calendar startCalendar, java.util.Calendar endCalendar){
        final com.google.api.services.calendar.Calendar service = new com.google.api.services.calendar.Calendar.Builder(httpTransport,jsonFactory,credentials).setApplicationName("FireRoad Scheduler").build();
        final com.google.api.services.calendar.model.Calendar calendar = new com.google.api.services.calendar.model.Calendar();
        calendar.setSummary("FireRoad Course Schedule");
        calendar.setDescription("Course Schedule Imported from FireRoad Android App");
        calendar.setTimeZone("America/New_York");
        final List<Event> events = getScheduleEvents(scheduleDocument,startCalendar,endCalendar);
        TaskDispatcher.perform(new TaskDispatcher.Task<com.google.api.services.calendar.model.Calendar>() {
            @Override
            public com.google.api.services.calendar.model.Calendar perform() {
                try {
                    return service.calendars().insert(calendar).execute();
                }catch (UserRecoverableAuthIOException ex){
                    mainActivity.startActivityForResult(ex.getIntent(), MainActivity.REQUEST_AUTHORIZATION);
                    return null;
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return null;
                }
            }
        }, new TaskDispatcher.CompletionBlock<com.google.api.services.calendar.model.Calendar>() {
            @Override
            public void completed(final com.google.api.services.calendar.model.Calendar arg) {
                if(arg != null) {
                    TaskDispatcher.inBackground(new TaskDispatcher.TaskNoReturn() {
                        @Override
                        public void perform() {
                            try {
                                for(Event event : events) {
                                    service.events().insert(arg.getId(), event).execute();
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    });
                }
            }
        });
    }
}
