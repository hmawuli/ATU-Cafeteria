import 'package:flutter/material.dart';
import '../services/api_service.dart';

class VendorStaffScreen extends StatefulWidget {
  const VendorStaffScreen({super.key});
  @override State<VendorStaffScreen> createState() => _VendorStaffScreenState();
}

class _VendorStaffScreenState extends State<VendorStaffScreen> {
  bool loading = true;
  List<dynamic> workers = [];
  Map<String,dynamic>? overview;
  List<dynamic> reviews = [];

  @override void initState() { super.initState(); _load(); }
  Future<void> _load() async { setState(() => loading = true); final o=await ApiService.vendorOverview(); final w=await ApiService.vendorWorkers(); final r=await ApiService.vendorReviews(); if(mounted)setState((){overview=o;workers=w;reviews=r;loading=false;}); }

  Future<void> _addWorker() async {
    final form=GlobalKey<FormState>(); final name=TextEditingController(), user=TextEditingController(), pin=TextEditingController(), pos=TextEditingController(text:'Staff'), phone=TextEditingController();
    final ok=await showDialog<bool>(context:context,builder:(_)=>AlertDialog(title:const Text('Add Worker'),content:Form(key:form,child:SingleChildScrollView(child:Column(children:[TextFormField(controller:name,decoration:const InputDecoration(labelText:'Full name'),validator:(v)=>v!.trim().isEmpty?'Required':null),TextFormField(controller:user,decoration:const InputDecoration(labelText:'Username'),validator:(v)=>v!.trim().isEmpty?'Required':null),TextFormField(controller:pin,obscureText:true,decoration:const InputDecoration(labelText:'Default PIN'),validator:(v)=>v!.length<4?'Minimum 4 characters':null),TextFormField(controller:pos,decoration:const InputDecoration(labelText:'Position')),TextFormField(controller:phone,decoration:const InputDecoration(labelText:'Phone (optional)'))]))),actions:[TextButton(onPressed:()=>Navigator.pop(context,false),child:const Text('Cancel')),ElevatedButton(onPressed:()=>form.currentState!.validate()?Navigator.pop(context,true):null,child:const Text('Create'))]));
    if(ok==true){final result=await ApiService.createWorker(fullName:name.text,username:user.text,pin:pin.text,position:pos.text,phone:phone.text.isEmpty?null:phone.text); if(!mounted)return; ScaffoldMessenger.of(context).showSnackBar(SnackBar(content:Text(result!=null?'Worker created successfully.':'Could not create worker.'))); _load();}
  }

  Future<void> _addShift(Map<String,dynamic> worker) async {
    final start=TextEditingController(text:'08:00'), end=TextEditingController(text:'16:00'), name=TextEditingController(text:'Regular');
    final ok=await showDialog<bool>(context:context,builder:(_)=>AlertDialog(title:Text('Shift — ${worker['full_name']}'),content:Column(mainAxisSize:MainAxisSize.min,children:[TextField(controller:name,decoration:const InputDecoration(labelText:'Shift name')),TextField(controller:start,decoration:const InputDecoration(labelText:'Start (HH:MM)')),TextField(controller:end,decoration:const InputDecoration(labelText:'End (HH:MM)'))]),actions:[TextButton(onPressed:()=>Navigator.pop(context,false),child:const Text('Cancel')),ElevatedButton(onPressed:()=>Navigator.pop(context,true),child:const Text('Save Shift'))]));
    if(ok==true){final id=(worker['id'] as num).toInt(); final result=await ApiService.createShift(workerId:id,startTime:start.text,endTime:end.text,shiftName:name.text,shiftDate:DateTime.now().toIso8601String().substring(0,10)); if(!mounted)return; ScaffoldMessenger.of(context).showSnackBar(SnackBar(content:Text(result!=null?'Shift scheduled.':'Could not schedule shift.'))); _load();}
  }

  @override Widget build(BuildContext context){
    final stats=overview?['statistics'] as Map<String,dynamic>?;
    return Scaffold(appBar:AppBar(title:const Text('Staff & Shift Management'),actions:[IconButton(onPressed:_load,icon:const Icon(Icons.refresh))]),floatingActionButton:FloatingActionButton.extended(onPressed:_addWorker,icon:const Icon(Icons.person_add),label:const Text('Add Worker')),body:loading?const Center(child:CircularProgressIndicator()):RefreshIndicator(onRefresh:_load,child:ListView(padding:const EdgeInsets.all(12),children:[
      Row(children:[Expanded(child:_stat('Revenue Today','GH₵ ${((stats?['revenue_today']??0) as num).toStringAsFixed(2)}')),Expanded(child:_stat('Workers','${stats?['workers']??workers.length}')),Expanded(child:_stat('Reviews','${stats?['reviews']??reviews.length}'))]),
      const SizedBox(height:16),const Text('YOUR WORKERS',style:TextStyle(fontWeight:FontWeight.bold)),const SizedBox(height:8),
      if(workers.isEmpty) const Card(child:Padding(padding:EdgeInsets.all(20),child:Text('No workers yet. Add your first staff member.'))),
      ...workers.map((w)=>Card(child:ExpansionTile(title:Text(w['full_name']??'Worker',style:const TextStyle(fontWeight:FontWeight.bold)),subtitle:Text('${w['position']??'Staff'} • ${w['username']??''} • ${w['is_active']==true?'Active':'Inactive'}'),children:[Padding(padding:const EdgeInsets.all(12),child:Column(crossAxisAlignment:CrossAxisAlignment.start,children:[Wrap(spacing:8,children:[ElevatedButton.icon(onPressed:()=>_addShift(Map<String,dynamic>.from(w)),icon:const Icon(Icons.schedule),label:const Text('Assign Shift')),OutlinedButton.icon(onPressed:()async{final ok=await ApiService.deleteWorker((w['id'] as num).toInt());if(ok)_load();},icon:const Icon(Icons.delete_outline),label:const Text('Remove'))]),const SizedBox(height:8),...List<dynamic>.from(w['shifts']??[]).map((s)=>ListTile(dense:true,leading:const Icon(Icons.access_time),title:Text('${s['shift_name']??'Shift'}: ${s['start_time']} – ${s['end_time']}'),subtitle:Text('${s['shift_date']??'Recurring'} • ${s['status']??'SCHEDULED'}'),trailing:IconButton(icon:const Icon(Icons.delete_outline),onPressed:()=>ApiService.deleteShift((s['id'] as num).toInt()).then((_)=>_load()))))]))])),
      const SizedBox(height:16),const Text('CUSTOMER REVIEWS',style:TextStyle(fontWeight:FontWeight.bold)),...reviews.take(20).map((r)=>ListTile(leading:const Icon(Icons.star),title:Text(r['comment']??'No comment'),subtitle:Text('Food ${r['rating_food_quality']??'-'} • Cleanliness ${r['rating_cleanliness']??'-'} • Service ${r['rating_service_speed']??'-'}')))
    ]));
  }
  Widget _stat(String label,String value)=>Card(child:Padding(padding:const EdgeInsets.all(12),child:Column(children:[Text(value,style:const TextStyle(fontSize:18,fontWeight:FontWeight.bold)),Text(label,style:const TextStyle(fontSize:11))])));
}
