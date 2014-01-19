package es.alexmj.travellist;


import java.util.ArrayList;

import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import es.alexmj.travellist.data.TravelsConstants;
import es.alexmj.travellist.data.TravelsDatabaseHelper;
import es.alexmj.travellist.data.TravelsProvider;

/**
 * Muestra las opciones para editar,crear,borrar y compartir un viaje. Pantalla principal
 *  de la aplicacion. 
 * VERSION 1: Este ejemplo muestra el uso de una clase ListActivity que muestra
 * 			una lista de paises visitados.
 * 			Para ello hacemos uso de una extension del ArrayAdapter que contiene una
 * 			lista de objetos TravelInfo. El metodo getView del adapter se encarga de
 * 			mostrar la informacion de cada entrada TravelInfo de la forma correcta en la
 * 			vista. 
 * VERSION 2: Anadimos en esta version los controles necesarios para lanzar las
 * 			activities correspondientes a mostrar un viaje mediante un click (metodo
 * 			onListItemClick) y para crear un viaje usando el menu de opciones
 * 			(onCreateOptionsMenu y onMenuItemSelected).
 * VERSION 3: Nuevas funcionalidades para que interactuen unas activities con
 * 			otras. Ahora es posible a�adir, borrar, editar y compartir un viaje. Se
 * 			aniade una base de datos que gestiona el almacenamiento de viajes.
 * VERSION 4: Aniadimos un provider para trabajar con la base de datos. Se aniade control por
 * 			  id de cada viaje.
 * VERSION 5: inclusion de la Action Bar para sustituir el boton de menu de opciones.
 * 
 * @author Alejandro.Marijuan@googlemail.com
 * 
 */
public class TravelListActivity extends ListActivity {

	private static final String TAG = "TravelListActivity: --->";
	private static final int REQUEST_CODE_NEW_TRIP = 1;
	private static final int REQUEST_CODE_EDIT_TRIP = 2;	
	// ##Para comunicar con la EditTravelActivity
	private String result;
	
	// ##VERSION 4: TravelsCursorAdapter
	private static TravelsDatabaseHelper dbHelper;
	private Uri newTravelUri;
	private TravelsCursorAdapter mAdapter;	
	private static final String[] PROJECTION = {TravelsConstants._ID, TravelsConstants.CITY, 
	TravelsConstants.COUNTRY, TravelsConstants.YEAR, TravelsConstants.NOTE};

	/**
	 * VERSION 4: Adapter que contiene la lista con los viajes del diario.
	 * 	Implementamos CursorAdapter para poder ejecutar el metodo changeCursor y actualizar el adapter,
	 *  ya que desde la versi�n SDK11 no se actualiza automaticamente con el metodo notify().
	 * @author Alejandro.Marijuan
	 *
	 */
	final class TravelsCursorAdapter extends CursorAdapter {

		private static final String TAG = "TravelsCursorAdapter: --->";
		private LayoutInflater mInflater;
		
		public TravelsCursorAdapter(Context context, Cursor c) {
			super(context, c, 0);
			mInflater=LayoutInflater.from(context);
		}

		/**
		 * Genera la nueva vista para una lista con dos elementos.
		 * @see android.widget.CursorAdapter#newView(android.content.Context, android.database.Cursor, android.view.ViewGroup)
		 */
		public View newView (Context context, Cursor cursor, ViewGroup parent){
			Log.d(TAG, "newView");
			//## En el metodo newView creamos la vista y el holder y lo almacenamos en el tag
			View view = mInflater.inflate(android.R.layout.simple_list_item_2, parent,false);
			ViewHolder holder = new ViewHolder();
			TextView textView1 = (TextView) view.findViewById(android.R.id.text1);
			TextView textView2 = (TextView) view.findViewById(android.R.id.text2);
			holder.text1 = textView1;
			holder.text2 = textView2;
			view.setTag(holder);
			return view;
			
		}// newView()
		
		/**
		 * Introduce los datos de cada elemento tipo Viaje en una posicion de la vista generada.
		 * @see android.widget.CursorAdapter#bindView(android.view.View, android.content.Context, android.database.Cursor)
		 */
		@Override
		public void bindView(View v, Context context, Cursor c) {
			Log.d(TAG, "bindView");
			//## En el metodo bindView recuperamos el Holder y asignamos los datos a las vistas
			ViewHolder holder = (ViewHolder) v.getTag();
			String id = c.getString(c.getColumnIndex(TravelsConstants._ID));
			String city = c.getString(c.getColumnIndex(TravelsConstants.CITY));
			String country = " ("+c.getString(c.getColumnIndex(TravelsConstants.COUNTRY))+")";
			String year = "A�o "+c.getInt(c.getColumnIndex(TravelsConstants.YEAR));
			Log.d(TAG,"("+id+")"+city+","+country+","+year);
			holder.text1.setText(city+country);
			holder.text2.setText(year);		
		}// bindView()
	}
	
	/**
	 * Vista para cada elemento de la lista.
	 * @author Alejandro.Marijuan
	 *
	 */
	static class ViewHolder {
		TextView text1;
		TextView text2;
	}// ViewHolder()

    /**
     * Genera la lista de viajes.
     * Genera un Intent para almacenar un viaje nuevo.
     * Asocia los menus contextuales a los controles.
     * VERSION 4: Introducimos el TravelsCursorAdapter, ordenando la lista de viajes por anyo.
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);        
        dbHelper = new TravelsDatabaseHelper(this);
        
        /**VERSION 4: TravelsCursorAdapter*/ 		        
        ContentResolver cr = getContentResolver();        
        //## Hacemos la consulta
        Cursor c = cr.query(TravelsProvider.CONTENT_URI,PROJECTION, null, null, TravelsConstants.YEAR+" DESC");
        //## y Construimos el Adapter con el cursor                
        mAdapter = new TravelsCursorAdapter(this,c);
        setListAdapter(mAdapter);
        //## Para devolver el resultado a la EditTravelActivity
		Intent returnIntent = new Intent();
		returnIntent.putExtra("result", result);
		setResult(RESULT_OK, returnIntent);
		//## Asociamos los menus contextuales a los controles
		registerForContextMenu(getListView());		
    }// onCreate()
    
    /**
     * Crea un menu de opciones, contiene la opcion generar un nuevo viaje.
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.d(TAG, "onCreateOptionsMenu");
		getMenuInflater().inflate(R.menu.travel_list_menu, menu);
		return true;
	}// onCreateOptionsMenu()

	/**
	 * Lanza un Intent para crear un nuevo viaje a EditTravelActivity.
	 * @see android.app.Activity#onMenuItemSelected(int, android.view.MenuItem)
	 */
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		Log.d(TAG, "onMenuItemSelected");
		switch (item.getItemId()) {
		case R.id.menu_new_travel:
			//## Creamos el Intent para lanzar la Activity EditTravelActivity
			Intent intent = new Intent(this, EditTravelActivity.class);		
			startActivityForResult(intent, REQUEST_CODE_NEW_TRIP);
			break;
		}		
		return super.onMenuItemSelected(featureId, item);
	}// onMenuItemSelected()

	/**
	 * Crea el Intent con los datos necesarios para mostrar un viaje por pantalla.
	 * @see android.app.ListActivity#onListItemClick(android.widget.ListView, android.view.View, int, long)
	 */
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Log.d(TAG, "onListItemClick");
		//## Tomamos la informacion del viaje seleccionado
		long row = mAdapter.getItemId(position);
		TravelInfo info = dbHelper.getTravelInfo((int) row);				
		//## Creamos el Intent para lanzar la Activity TravelActivity
		Intent intent = new Intent(this, TravelActivity.class);
		//## Aniadimos como extras los datos que consideremos necesarios para la
		//## Activity a lanzar
		
		intent.putExtra(TravelInfo.EXTRA_ID, row);
		intent.putExtra(TravelInfo.EXTRA_CITY, info.getCity());
		intent.putExtra(TravelInfo.EXTRA_COUNTRY, info.getCountry());
		intent.putExtra(TravelInfo.EXTRA_YEAR, info.getYear());
		intent.putExtra(TravelInfo.EXTRA_NOTE, info.getNote());
			Log.w(TAG,"("+row+")"+info.getCity()+","+info.getCountry()+",("+info.getYear()+"),"+  info.getNote() );
			
		//## Lanzamos la Activity con el Intent creado a TravelActivity
		startActivity(intent);
		super.onListItemClick(l, v, position, id);
	}// onListItemClick()

	/**
	 * Recoge los datos de un viaje editado.
	 *  Diferenciamos el metodo en funcion de si es un viaje nuevo o editado.
	 * @see android.support.v4.app.FragmentActivity#onActivityResult(int, int,
	 * android.content.Intent)
	 */
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult");
		super.onActivityResult(requestCode, resultCode, data);
		boolean newTripToAdd=false;
		int myBiggestID=0;
		
		//**************Seccion para un nuevo viaje***********************//		
		if (resultCode == RESULT_OK	&& data.getExtras().containsKey("myTripCreated")) {
			Log.i(TAG, "RESULT_OK -- Nuevo viaje!");
			String[] resultsTrip = data.getExtras().getStringArray("myTripCreated");
			int myNewTripID=Integer.valueOf(resultsTrip[0]);
			if(myNewTripID==0){				
				//## Obtener el id mas grande de la BD y sumarle 1
				myBiggestID=dbHelper.getLastId()+1;
					Log.w(TAG,"##################### CHANGE idTrip with:"+myBiggestID);
				myNewTripID=myBiggestID;
				newTripToAdd=true;
			}
			String myNewTripCity = resultsTrip[1];
			String myNewTripCountry = resultsTrip[2];
			int myNewTripYear = Integer.valueOf(resultsTrip[3]);
			String myNewTripNote = resultsTrip[4];
			
			if(newTripToAdd){
				Log.i(TAG, "A�adimos al adapter la info del viaje");
			    ContentValues valuesNewTrip = new ContentValues();
			    valuesNewTrip.put(TravelsConstants._ID, myNewTripID);
			    valuesNewTrip.put(TravelsConstants.CITY, myNewTripCity);
			    valuesNewTrip.put(TravelsConstants.COUNTRY, myNewTripCountry);
			    valuesNewTrip.put(TravelsConstants.YEAR, myNewTripYear);
			    valuesNewTrip.put(TravelsConstants.NOTE, myNewTripNote);

			    if (newTravelUri == null) {
			      //## Nuevo viaje
			    	newTravelUri = getContentResolver().insert(TravelsProvider.CONTENT_URI, valuesNewTrip);
			    }
			    else{
			    	getContentResolver().insert(TravelsProvider.CONTENT_URI, valuesNewTrip);			    	
			    }			    
			  }
			//**************Seccion para edicion de un viaje***********************//	
			if (requestCode == REQUEST_CODE_EDIT_TRIP) {
				Log.i(TAG, "REQUEST_CODE_EDIT_TRIP: Viaje Editado!");
				Log.w(TAG, "info del Viaje Editado:"+myNewTripID+"-"+myNewTripCity+"("+myNewTripCountry+"),"+myNewTripYear+"--"+myNewTripNote);				
				ContentValues valuesTripEdited = new ContentValues();
				valuesTripEdited.put(TravelsConstants.CITY, myNewTripCity);
				valuesTripEdited.put(TravelsConstants.COUNTRY, myNewTripCountry);
				valuesTripEdited.put(TravelsConstants.YEAR, myNewTripYear);
				valuesTripEdited.put(TravelsConstants.NOTE, myNewTripNote);
				//## Update viaje en el Provider
				getContentResolver().update(TravelsProvider.CONTENT_URI, valuesTripEdited, TravelsConstants._ID+"="+myNewTripID, null);				
			}			
			//## VERSION 4: Cambiamos el ResourceCursorAdapter por CursorAdapter para poder actualizar el adapter.
			ContentResolver cr = getContentResolver();
			Cursor c = cr.query(TravelsProvider.CONTENT_URI,PROJECTION, null, null, TravelsConstants.YEAR+" DESC");
	    	mAdapter.changeCursor(c);
	    	Toast.makeText(TravelListActivity.this, "Guardado viaje a "+myNewTripCity+"("+myNewTripYear+")", Toast.LENGTH_LONG).show();
		}
		else {
			Log.i(TAG, "ACCI�N CANCELADA " + resultCode + "-" + requestCode);
		}

	}// onActivityResult()

	/**
	 * Crea un menu con opciones para pulsacion larga sobre un item viaje.
	 * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu,
	 * android.view.View, android.view.ContextMenu.ContextMenuInfo)
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		Log.d(TAG, "onCreateContextMenu");
		super.onCreateContextMenu(menu, v, menuInfo);
		Log.i(TAG, "pulsaci�n larga...");
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.context_menu, menu);
		menu.setHeaderTitle(R.string.menuContx);
	}// onCreateContextMenu()

	/**
     * Obtiene los datos de la tabla de la BBDD y los almacena en una lista.
     *  Lo utilizamos en la VERSION 4 de la app.
     * @return lista de viajes
     */
	public ArrayList<TravelInfo> getTravelsList() {
		Log.d(TAG, "getTravelsList");
		ArrayList<TravelInfo> travels = new ArrayList<TravelInfo>();
		Cursor c = getContentResolver().query(TravelsProvider.CONTENT_URI,null, null, null, TravelsConstants.YEAR+" DESC");
		if (c != null && c.moveToFirst()) {
			int idDBIndex = c.getColumnIndex(TravelsConstants._ID);
			int cityIndex = c.getColumnIndex(TravelsConstants.CITY);
			int countryIndex = c.getColumnIndex(TravelsConstants.COUNTRY);
			int yearIndex = c.getColumnIndex(TravelsConstants.YEAR);
			int noteIndex = c.getColumnIndex(TravelsConstants.NOTE);
			do {
				int idDB = c.getInt(idDBIndex);
				String city = c.getString(cityIndex);
				String country = c.getString(countryIndex);
				int year = c.getInt(yearIndex);
				String note = c.getString(noteIndex);
					Log.i(TAG,"%%%%%%%%%%%%%%%%%%%%% idDB="+idDB);
				TravelInfo travel = new TravelInfo(idDB, city, country, year, note);
				travels.add(travel);
			} while (c.moveToNext());
			c.close();
		}
    	Log.i(TAG, "Nro de viajes en DB: "+ travels.size());
		return travels;
	}// getTravelsList()
    
	/**
	 * Genera las opciones de COMPARTIR,EDITAR,BORRAR.
	 * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		Log.d(TAG, "onContextItemSelected");
		AdapterView.AdapterContextMenuInfo info;
		try {
			info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		} catch (ClassCastException e) {
			return false;
		}
		long row = getListAdapter().getItemId(info.position);
		Log.i(TAG, "ListAdapter id = " + row + "---" + item.getTitle());

		switch (item.getItemId()) {
		case R.id.CtxLblOpc1:
			Log.i(TAG, "Etiqueta: Opcion 1 pulsada!--COMPARTIR");
			//## Creamos el Intent para lanzar la Activity que permita compartir el viaje
			Intent sendIntent = new Intent();
			sendIntent.setAction(Intent.ACTION_SEND);
			TravelInfo myTrip = dbHelper.getTravelInfo((int) row);
			//## Obtenemos los datos para incluirlos en el intent.			
			String cityResult = myTrip.getCity();
			String countryResult = myTrip.getCountry();
			Integer yearResult = myTrip.getYear();
			String noteResult = myTrip.getNote();
			//## Creamos la la forma en que quedar� representado el viaje al compartirlo
			if (noteResult == null)
				noteResult = getResources().getString(R.string.emptyNote);
			sendIntent.putExtra(Intent.EXTRA_TEXT,cityResult + "("
							+ countryResult + ")\n" + "A�o: "
							+ yearResult.toString() + "\n" + "Nota: "
							+ noteResult);
			sendIntent.setType("text/plain");
			startActivity(Intent.createChooser(sendIntent, getResources()
					.getText(R.string.send_to)));
			return true;
		case R.id.CtxLblOpc2:
			Log.i(TAG, "Etiqueta: Opcion 2 pulsada!--EDITAR");
			//## Creamos el Intent para lanzar la Activity EditTravelActivity
			Intent intent = new Intent(this, EditTravelActivity.class);
			TravelInfo myTrip4Edit = dbHelper.getTravelInfo((int) row);
			//## Obtenemos los datos para incluirlos en el Intent
			int idDB4edit = myTrip4Edit.getIdDB();
			String cityResult4edit = myTrip4Edit.getCity();
			String countryResult4edit = myTrip4Edit.getCountry();
			Integer yearResult4edit = myTrip4Edit.getYear();
			String noteResult4edit = myTrip4Edit.getNote();
			Integer originalTripPosition = idDB4edit;
			if (noteResult4edit == null)
				noteResult4edit = getResources().getString(R.string.emptyNote);			
			intent.putExtra("SavedDataTripCity", cityResult4edit);
			intent.putExtra("SavedDataTripCountry", countryResult4edit);
			intent.putExtra("SavedDataTripYear", yearResult4edit);
			intent.putExtra("SavedDataTripNote", noteResult4edit);
			intent.putExtra("TripId", originalTripPosition);
			Log.i(TAG, "info del viaje para editar: " + cityResult4edit + ","
					+ countryResult4edit + "," + yearResult4edit.toString()
					+ "," + noteResult4edit + "," + originalTripPosition);
			startActivityForResult(intent, REQUEST_CODE_EDIT_TRIP);			
			return true;
		case R.id.CtxLblOpc3:
				Log.i(TAG, "Etiqueta: Opcion 3 pulsada!--BORRAR");
				Log.i(TAG, "---Nro de viajes antes de BORRAR (DB): "+ dbHelper.getTravelsList().size());
			TravelInfo myTrip4Delete = dbHelper.getTravelInfo((int) row);
			int idDB4delete=myTrip4Delete.getIdDB();
			String cityResult4delete = myTrip4Delete.getCountry();
			Integer yearResult4delete = myTrip4Delete.getYear();
				Log.i(TAG, "---idDB4delete: "+ idDB4delete);			
			Uri uri = Uri.parse(TravelsProvider.CONTENT_URI + "/" + idDB4delete);
			getContentResolver().delete(uri, null, null);
			//## VERSION 4: Cambiamos el ResourceCursorAdapter por CursorAdapter para poder actualizar el adapter.
			ContentResolver cr = getContentResolver();
			Cursor c = cr.query(TravelsProvider.CONTENT_URI,PROJECTION, null, null, TravelsConstants.YEAR+" DESC");
	    	mAdapter.changeCursor(c);				
				Log.i(TAG, "---Numero de viajes despues de BORRAR (DB): "+ dbHelper.getTravelsList().size());
			Toast.makeText(TravelListActivity.this, "Borrado viaje a "+cityResult4delete+"("+yearResult4delete+")", Toast.LENGTH_LONG).show();
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}// onContextItemSelected()

	
}
