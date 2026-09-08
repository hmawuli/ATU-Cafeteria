import 'package:flutter/material.dart';
import '../services/api_service.dart';

class AdminOversightScreen extends StatefulWidget {
  const AdminOversightScreen({super.key});
  @override State<AdminOversightScreen> createState()=>_AdminOversightScreenState();
}
class _AdminOversightScreenState extends State<AdminOversightScreen>{
  bool loading=true; Map<String,dynamic>? data;
  @override void initState(){super.initState();_load();}
  Future<void> _load() async {setState(()=>loading=true);final d=await ApiService.adminOverview();if(mounted)setState((){data=d;loading=false;});}
  @override Widget build(BuildContext context){
    final s=Map<String,dynamic>.from(data?['statistics']??{}); final vendors=List<dynamic>.from(data?['vendor_summary']??[]); final workers=List<dynamic>.from(data?['workers']??[]); final shifts=List<dynamic>.from(data?['shifts_today']??[]); final activity=List<dynamic>.from(data?['recent_activity']??[]);
    return Scaffold(appBar:AppBar(title:const Text('System Oversight'),actions:[IconButton(onPressed:_load,icon:const Icon(Icons.refresh))]),body:loading?const Center(child:CircularProgressIndicator()):RefreshIndicator(onRefresh:_load,child:ListView(padding:const EdgeInsets.all(12),children:[
      GridView.count(crossAxisCount:2,shrinkWrap:true,physics:const NeverScrollableScrollPhysics(),childAspectRatio:2.2,children:[_stat('Vendors','${s['vendors']??0}'),_stat('Workers','${s['workers']??0}'),_stat('Orders','${s['orders']??0}'),_stat('Revenue','GH₵ ${((s['revenue']??0) as num).toStringAsFixed(2)}'),_stat('Today','GH₵ ${((s['revenue_today']??0) as num).toStringAsFixed(2)}'),_stat('Shifts Today','${s['shifts_today']??0}')]),
      const SizedBox(height:16),const Text('VENDOR PERFORMANCE',style:TextStyle(fontWeight:FontWeight.bold)),...vendors.map((v)=>Card(child:ListTile(title:Text(v['fullName']??''),subtitle:Text('${v['workers']} workers • ${v['orders']} orders • ${v['completed_orders']} completed'),trailing:Text('GH₵ ${((v['revenue']??0) as num).toStringAsFixed(2)}',style:const TextStyle(fontWeight:FontWeight.bold)))),
      const SizedBox(height:16),const Text('TODAY\'S SHIFTS',style:TextStyle(fontWeight:FontWeight.bold)),if(shifts.isEmpty)const ListTile(title:Text('No shifts scheduled today.')),...shifts.map((x)=>ListTile(leading:const Icon(Icons.schedule),title:Text(x['worker']?['full_name']??'Worker'),subtitle:Text('${x['start_time']} – ${x['end_time']} • ${x['vendor']?['fullName']??''}'))),
      const SizedBox(height:16),const Text('RECENT SYSTEM ACTIVITY',style:TextStyle(fontWeight:FontWeight.bold)),...activity.take(30).map((a)=>ListTile(dense:true,leading:const Icon(Icons.security,size:18),title:Text(a['action']??''),subtitle:Text(a['details']??'')))
    ])));
  }
  Widget _stat(String a,String b)=>Card(child:Padding(padding:const EdgeInsets.all(10),child:Column(mainAxisAlignment:MainAxisAlignment.center,children:[Text(b,style:const TextStyle(fontWeight:FontWeight.bold,fontSize:17)),Text(a,style:const TextStyle(fontSize:11))])));
}
