/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class org_firebirdsql_ngds_GDS_0005fImpl */

#ifndef _Included_org_firebirdsql_ngds_GDS_0005fImpl
#define _Included_org_firebirdsql_ngds_GDS_0005fImpl
#ifdef __cplusplus
extern "C" {
#endif
/* Inaccessible static: log */
/* Inaccessible static: LIST_OF_CLIENT_LIBRARIES_TO_TRY */
/* Inaccessible static: LIST_OF_EMBEDDED_SERVER_LIBRARIES_TO_TRY */
/* Inaccessible static: describe_database_info */
/* Inaccessible static: stmtInfo */
/* Inaccessible static: INFO_SIZE */
/* Inaccessible static: class_00024org_00024firebirdsql_00024ngds_00024GDS_Impl */
/*
 * Class:     org_firebirdsql_ngds_GDS_0005fImpl
 * Method:    nativeInitilize
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_org_firebirdsql_ngds_GDS_1Impl_nativeInitilize
  (JNIEnv *, jobject, jstring);

/*
 * Class:     org_firebirdsql_ngds_GDS_0005fImpl
 * Method:    native_isc_create_database
 * Signature: (Ljava/lang/String;Lorg/firebirdsql/gds/isc_db_handle;[B)V
 */
JNIEXPORT void JNICALL Java_org_firebirdsql_ngds_GDS_1Impl_native_1isc_1create_1database
  (JNIEnv *, jobject, jstring, jobject, jbyteArray);

/*
 * Class:     org_firebirdsql_ngds_GDS_0005fImpl
 * Method:    native_isc_attach_database
 * Signature: (Ljava/lang/String;Lorg/firebirdsql/gds/isc_db_handle;[B)V
 */
JNIEXPORT void JNICALL Java_org_firebirdsql_ngds_GDS_1Impl_native_1isc_1attach_1database
  (JNIEnv *, jobject, jstring, jobject, jbyteArray);

/*
 * Class:     org_firebirdsql_ngds_GDS_0005fImpl
 * Method:    native_isc_database_info
 * Signature: (Lorg/firebirdsql/gds/isc_db_handle;I[BI[B)V
 */
JNIEXPORT void JNICALL Java_org_firebirdsql_ngds_GDS_1Impl_native_1isc_1database_1info
  (JNIEnv *, jobject, jobject, jint, jbyteArray, jint, jbyteArray);

/*
 * Class:     org_firebirdsql_ngds_GDS_0005fImpl
 * Method:    native_isc_detach_database
 * Signature: (Lorg/firebirdsql/gds/isc_db_handle;)V
 */
JNIEXPORT void JNICALL Java_org_firebirdsql_ngds_GDS_1Impl_native_1isc_1detach_1database
  (JNIEnv *, jobject, jobject);

/*
 * Class:     org_firebirdsql_ngds_GDS_0005fImpl
 * Method:    native_isc_drop_database
 * Signature: (Lorg/firebirdsql/gds/isc_db_handle;)V
 */
JNIEXPORT void JNICALL Java_org_firebirdsql_ngds_GDS_1Impl_native_1isc_1drop_1database
  (JNIEnv *, jobject, jobject);

/*
 * Class:     org_firebirdsql_ngds_GDS_0005fImpl
 * Method:    native_isc_start_transaction
 * Signature: (Lorg/firebirdsql/gds/isc_tr_handle;Lorg/firebirdsql/gds/isc_db_handle;[B)V
 */
JNIEXPORT void JNICALL Java_org_firebirdsql_ngds_GDS_1Impl_native_1isc_1start_1transaction
  (JNIEnv *, jobject, jobject, jobject, jbyteArray);

/*
 * Class:     org_firebirdsql_ngds_GDS_0005fImpl
 * Method:    native_isc_commit_transaction
 * Signature: (Lorg/firebirdsql/gds/isc_tr_handle;)V
 */
JNIEXPORT void JNICALL Java_org_firebirdsql_ngds_GDS_1Impl_native_1isc_1commit_1transaction
  (JNIEnv *, jobject, jobject);

/*
 * Class:     org_firebirdsql_ngds_GDS_0005fImpl
 * Method:    native_isc_rollback_transaction
 * Signature: (Lorg/firebirdsql/gds/isc_tr_handle;)V
 */
JNIEXPORT void JNICALL Java_org_firebirdsql_ngds_GDS_1Impl_native_1isc_1rollback_1transaction
  (JNIEnv *, jobject, jobject);

/*
 * Class:     org_firebirdsql_ngds_GDS_0005fImpl
 * Method:    native_isc_commit_retaining
 * Signature: (Lorg/firebirdsql/gds/isc_tr_handle;)V
 */
JNIEXPORT void JNICALL Java_org_firebirdsql_ngds_GDS_1Impl_native_1isc_1commit_1retaining
  (JNIEnv *, jobject, jobject);

/*
 * Class:     org_firebirdsql_ngds_GDS_0005fImpl
 * Method:    native_isc_prepare_transaction
 * Signature: (Lorg/firebirdsql/gds/isc_tr_handle;)V
 */
JNIEXPORT void JNICALL Java_org_firebirdsql_ngds_GDS_1Impl_native_1isc_1prepare_1transaction
  (JNIEnv *, jobject, jobject);

/*
 * Class:     org_firebirdsql_ngds_GDS_0005fImpl
 * Method:    native_isc_prepare_transaction2
 * Signature: (Lorg/firebirdsql/gds/isc_tr_handle;[B)V
 */
JNIEXPORT void JNICALL Java_org_firebirdsql_ngds_GDS_1Impl_native_1isc_1prepare_1transaction2
  (JNIEnv *, jobject, jobject, jbyteArray);

/*
 * Class:     org_firebirdsql_ngds_GDS_0005fImpl
 * Method:    native_isc_rollback_retaining
 * Signature: (Lorg/firebirdsql/gds/isc_tr_handle;)V
 */
JNIEXPORT void JNICALL Java_org_firebirdsql_ngds_GDS_1Impl_native_1isc_1rollback_1retaining
  (JNIEnv *, jobject, jobject);

/*
 * Class:     org_firebirdsql_ngds_GDS_0005fImpl
 * Method:    native_isc_dsql_allocate_statement
 * Signature: (Lorg/firebirdsql/gds/isc_db_handle;Lorg/firebirdsql/gds/isc_stmt_handle;)V
 */
JNIEXPORT void JNICALL Java_org_firebirdsql_ngds_GDS_1Impl_native_1isc_1dsql_1allocate_1statement
  (JNIEnv *, jobject, jobject, jobject);

/*
 * Class:     org_firebirdsql_ngds_GDS_0005fImpl
 * Method:    native_isc_dsql_alloc_statement2
 * Signature: (Lorg/firebirdsql/gds/isc_db_handle;Lorg/firebirdsql/gds/isc_stmt_handle;)V
 */
JNIEXPORT void JNICALL Java_org_firebirdsql_ngds_GDS_1Impl_native_1isc_1dsql_1alloc_1statement2
  (JNIEnv *, jobject, jobject, jobject);

/*
 * Class:     org_firebirdsql_ngds_GDS_0005fImpl
 * Method:    native_isc_dsql_describe
 * Signature: (Lorg/firebirdsql/gds/isc_stmt_handle;I)Lorg/firebirdsql/gds/XSQLDA;
 */
JNIEXPORT jobject JNICALL Java_org_firebirdsql_ngds_GDS_1Impl_native_1isc_1dsql_1describe
  (JNIEnv *, jobject, jobject, jint);

/*
 * Class:     org_firebirdsql_ngds_GDS_0005fImpl
 * Method:    native_isc_dsql_describe_bind
 * Signature: (Lorg/firebirdsql/gds/isc_stmt_handle;I)Lorg/firebirdsql/gds/XSQLDA;
 */
JNIEXPORT jobject JNICALL Java_org_firebirdsql_ngds_GDS_1Impl_native_1isc_1dsql_1describe_1bind
  (JNIEnv *, jobject, jobject, jint);

/*
 * Class:     org_firebirdsql_ngds_GDS_0005fImpl
 * Method:    native_isc_dsql_execute2
 * Signature: (Lorg/firebirdsql/gds/isc_tr_handle;Lorg/firebirdsql/gds/isc_stmt_handle;ILorg/firebirdsql/gds/XSQLDA;Lorg/firebirdsql/gds/XSQLDA;)V
 */
JNIEXPORT void JNICALL Java_org_firebirdsql_ngds_GDS_1Impl_native_1isc_1dsql_1execute2
  (JNIEnv *, jobject, jobject, jobject, jint, jobject, jobject);

/*
 * Class:     org_firebirdsql_ngds_GDS_0005fImpl
 * Method:    native_isc_dsql_exec_immed2
 * Signature: (Lorg/firebirdsql/gds/isc_db_handle;Lorg/firebirdsql/gds/isc_tr_handle;[BILorg/firebirdsql/gds/XSQLDA;Lorg/firebirdsql/gds/XSQLDA;)V
 */
JNIEXPORT void JNICALL Java_org_firebirdsql_ngds_GDS_1Impl_native_1isc_1dsql_1exec_1immed2
  (JNIEnv *, jobject, jobject, jobject, jbyteArray, jint, jobject, jobject);

/*
 * Class:     org_firebirdsql_ngds_GDS_0005fImpl
 * Method:    native_isc_dsql_fetch
 * Signature: (Lorg/firebirdsql/gds/isc_stmt_handle;ILorg/firebirdsql/gds/XSQLDA;I)Z
 */
JNIEXPORT jboolean JNICALL Java_org_firebirdsql_ngds_GDS_1Impl_native_1isc_1dsql_1fetch
  (JNIEnv *, jobject, jobject, jint, jobject, jint);

/*
 * Class:     org_firebirdsql_ngds_GDS_0005fImpl
 * Method:    native_isc_dsql_free_statement
 * Signature: (Lorg/firebirdsql/gds/isc_stmt_handle;I)V
 */
JNIEXPORT void JNICALL Java_org_firebirdsql_ngds_GDS_1Impl_native_1isc_1dsql_1free_1statement
  (JNIEnv *, jobject, jobject, jint);

/*
 * Class:     org_firebirdsql_ngds_GDS_0005fImpl
 * Method:    native_isc_dsql_prepare
 * Signature: (Lorg/firebirdsql/gds/isc_tr_handle;Lorg/firebirdsql/gds/isc_stmt_handle;[BI)Lorg/firebirdsql/gds/XSQLDA;
 */
JNIEXPORT jobject JNICALL Java_org_firebirdsql_ngds_GDS_1Impl_native_1isc_1dsql_1prepare
  (JNIEnv *, jobject, jobject, jobject, jbyteArray, jint);

/*
 * Class:     org_firebirdsql_ngds_GDS_0005fImpl
 * Method:    native_isc_dsql_set_cursor_name
 * Signature: (Lorg/firebirdsql/gds/isc_stmt_handle;Ljava/lang/String;I)V
 */
JNIEXPORT void JNICALL Java_org_firebirdsql_ngds_GDS_1Impl_native_1isc_1dsql_1set_1cursor_1name
  (JNIEnv *, jobject, jobject, jstring, jint);

/*
 * Class:     org_firebirdsql_ngds_GDS_0005fImpl
 * Method:    native_isc_dsql_sql_info
 * Signature: (Lorg/firebirdsql/gds/isc_stmt_handle;[BI)[B
 */
JNIEXPORT jbyteArray JNICALL Java_org_firebirdsql_ngds_GDS_1Impl_native_1isc_1dsql_1sql_1info
  (JNIEnv *, jobject, jobject, jbyteArray, jint);

/*
 * Class:     org_firebirdsql_ngds_GDS_0005fImpl
 * Method:    native_isc_create_blob2
 * Signature: (Lorg/firebirdsql/gds/isc_db_handle;Lorg/firebirdsql/gds/isc_tr_handle;Lorg/firebirdsql/gds/isc_blob_handle;[B)V
 */
JNIEXPORT void JNICALL Java_org_firebirdsql_ngds_GDS_1Impl_native_1isc_1create_1blob2
  (JNIEnv *, jobject, jobject, jobject, jobject, jbyteArray);

/*
 * Class:     org_firebirdsql_ngds_GDS_0005fImpl
 * Method:    native_isc_open_blob2
 * Signature: (Lorg/firebirdsql/gds/isc_db_handle;Lorg/firebirdsql/gds/isc_tr_handle;Lorg/firebirdsql/gds/isc_blob_handle;[B)V
 */
JNIEXPORT void JNICALL Java_org_firebirdsql_ngds_GDS_1Impl_native_1isc_1open_1blob2
  (JNIEnv *, jobject, jobject, jobject, jobject, jbyteArray);

/*
 * Class:     org_firebirdsql_ngds_GDS_0005fImpl
 * Method:    native_isc_get_segment
 * Signature: (Lorg/firebirdsql/gds/isc_blob_handle;I)[B
 */
JNIEXPORT jbyteArray JNICALL Java_org_firebirdsql_ngds_GDS_1Impl_native_1isc_1get_1segment
  (JNIEnv *, jobject, jobject, jint);

/*
 * Class:     org_firebirdsql_ngds_GDS_0005fImpl
 * Method:    native_isc_put_segment
 * Signature: (Lorg/firebirdsql/gds/isc_blob_handle;[B)V
 */
JNIEXPORT void JNICALL Java_org_firebirdsql_ngds_GDS_1Impl_native_1isc_1put_1segment
  (JNIEnv *, jobject, jobject, jbyteArray);

/*
 * Class:     org_firebirdsql_ngds_GDS_0005fImpl
 * Method:    native_isc_close_blob
 * Signature: (Lorg/firebirdsql/gds/isc_blob_handle;)V
 */
JNIEXPORT void JNICALL Java_org_firebirdsql_ngds_GDS_1Impl_native_1isc_1close_1blob
  (JNIEnv *, jobject, jobject);

/*
 * Class:     org_firebirdsql_ngds_GDS_0005fImpl
 * Method:    native_isc_blob_info
 * Signature: (Lorg/firebirdsql/ngds/isc_blob_handle_impl;[BI)[B
 */
JNIEXPORT jbyteArray JNICALL Java_org_firebirdsql_ngds_GDS_1Impl_native_1isc_1blob_1info
  (JNIEnv *, jobject, jobject, jbyteArray, jint);

/*
 * Class:     org_firebirdsql_ngds_GDS_0005fImpl
 * Method:    native_isc_seek_blob
 * Signature: (Lorg/firebirdsql/ngds/isc_blob_handle_impl;II)V
 */
JNIEXPORT void JNICALL Java_org_firebirdsql_ngds_GDS_1Impl_native_1isc_1seek_1blob
  (JNIEnv *, jobject, jobject, jint, jint);

/*
 * Class:     org_firebirdsql_ngds_GDS_0005fImpl
 * Method:    native_isc_service_attach
 * Signature: (Ljava/lang/String;Lorg/firebirdsql/gds/isc_svc_handle;[B)V
 */
JNIEXPORT void JNICALL Java_org_firebirdsql_ngds_GDS_1Impl_native_1isc_1service_1attach
  (JNIEnv *, jobject, jstring, jobject, jbyteArray);

/*
 * Class:     org_firebirdsql_ngds_GDS_0005fImpl
 * Method:    native_isc_service_detach
 * Signature: (Lorg/firebirdsql/gds/isc_svc_handle;)V
 */
JNIEXPORT void JNICALL Java_org_firebirdsql_ngds_GDS_1Impl_native_1isc_1service_1detach
  (JNIEnv *, jobject, jobject);

/*
 * Class:     org_firebirdsql_ngds_GDS_0005fImpl
 * Method:    native_isc_service_start
 * Signature: (Lorg/firebirdsql/gds/isc_svc_handle;[B)V
 */
JNIEXPORT void JNICALL Java_org_firebirdsql_ngds_GDS_1Impl_native_1isc_1service_1start
  (JNIEnv *, jobject, jobject, jbyteArray);

/*
 * Class:     org_firebirdsql_ngds_GDS_0005fImpl
 * Method:    native_isc_service_query
 * Signature: (Lorg/firebirdsql/gds/isc_svc_handle;[B[B[B)V
 */
JNIEXPORT void JNICALL Java_org_firebirdsql_ngds_GDS_1Impl_native_1isc_1service_1query
  (JNIEnv *, jobject, jobject, jbyteArray, jbyteArray, jbyteArray);

#ifdef __cplusplus
}
#endif
#endif