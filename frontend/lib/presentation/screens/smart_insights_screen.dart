import 'package:flutter/material.dart';
import 'package:atu_cafeteria/core/network/api_client.dart';
import 'package:atu_cafeteria/presentation/providers/cafeteria_provider.dart';
import 'package:provider/provider.dart';

class PerformanceInsightsScreen extends StatefulWidget {
  const PerformanceInsightsScreen({super.key});
  @override State<SmartInsightsScreen> createState() => _PerformanceInsightsScreenState();
}

class _PerformanceInsightsScreenState extends State<SmartInsightsScreen> {
  final ApiClient _api = ApiClient();
  bool loading=true; String? error; List<dynamic> recommendations=[]; List<dynamic> forecast=[]; Map<String,dynamic> waste={}; Map<String,dynamic> adminData={};

  @override void initState(){ super.initState(); WidgetsBinding.instance.addPostFrameCallback((_)=>_load()); }
  Future<void> _load() async {
    final cafe=context.read<CafeteriaProvider>(); _api.token=cafe.authToken;
    setState(()=>loading=true);
    try {
      final role=(cafe.currentUser?.role??'').toUpperCase();
      if(role=='ADMIN') { final r=await _api.request('GET','admin/command-center'); adminData=Map<String,dynamic>.from(r['data']??{}); }
      if(role=='STUDENT') { final r=await _api.request('GET','student/recommendations'); recommendations=List<dynamic>.from(r['data']??[]); }
      if(role=='VENDOR') { final r=await _api.request('GET','vendor/demand-forecast'); forecast=List<dynamic>.from(r['data']??[]); final w=await _api.request('GET','vendor/waste/summary'); waste=Map<String,dynamic>.from(w['data']??{}); }
      setState(()=>error=null);
    } catch(e){setState(()=>error=e.toString());} finally{if(mounted)setState(()=>loading=false);}
  }
  @override void dispose(){_api.close();super.dispose();}
  @override Widget build(BuildContext context){
    final role=(context.watch<CafeteriaProvider>().currentUser?.role??'').toUpperCase();
    return Scaffold(appBar:AppBar(title:Text(role=='STUDENT'?'Meal Recommendations':role=='VENDOR'?'Demand & Waste':'Cafeteria Performance'),actions:[IconButton(onPressed:_load,icon:const Icon(Icons.refresh))]),body:loading?const Center(child:CircularProgressIndicator()):error!=null?Center(child:Padding(padding:const EdgeInsets.all(20),child:Text(error!,textAlign:TextAlign.center))):role=='ADMIN'?_admin():role=='STUDENT'?_student():_vendor());
  }
  Widget _admin()=>ListView(padding:const EdgeInsets.all(16),children:[const Text('Cafeteria performance',style:TextStyle(fontSize:22,fontWeight:FontWeight.bold)),const SizedBox(height:16),...adminData.entries.map((e)=>Card(child:ListTile(title:Text(e.key.replaceAll('_',' ')),trailing:Text('${e.value}'))))]);
  Widget _student()=>ListView(padding:const EdgeInsets.all(16),children:[const Text('Recommended meals',style:TextStyle(fontSize:22,fontWeight:FontWeight.bold)),const SizedBox(height:8),const Text('Suggestions use your order history and current availability.'),const SizedBox(height:16),...recommendations.map((x)=>Card(child:ListTile(leading:const Icon(Icons.restaurant),title:Text('${x['name']??'Food'}'),subtitle:Text('${x['category']??'General'} • Available now'),trailing:Text('GHS ${x['price']??0}'))))]);
  Widget _vendor()=>ListView(padding:const EdgeInsets.all(16),children:[const Text('Demand forecast',style:TextStyle(fontSize:22,fontWeight:FontWeight.bold)),const SizedBox(height:8),const Text('Based on recent cafeteria demand data for the next operating day.'),const SizedBox(height:16),...forecast.map((x)=>Card(child:ListTile(title:Text('${x['food_name']??'Food'}'),subtitle:Text('Confidence ${x['confidence']??0}%'),trailing:Text('${x['predicted_quantity']??0} portions')))),const SizedBox(height:16),Card(child:ListTile(leading:const Icon(Icons.eco),title:const Text('30-day food waste'),subtitle:Text('Prepared ${waste['prepared']??0} • Sold ${waste['sold']??0} • Wasted ${waste['wasted']??0}'),trailing:Text('${waste['waste_rate_percent']??0}%')))]);
}
