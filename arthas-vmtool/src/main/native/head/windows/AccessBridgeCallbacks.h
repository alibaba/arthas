/*
 * Copyright (c) 2005, 2010, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

/*
 * AccessBridgeCallbacks.h      1.17 05/03/21
 */

/*
 * Header file defining callback typedefs for Windows routines
 * which are called from Java (responding to events, etc.).
 */

#ifndef __AccessBridgeCallbacks_H__
#define __AccessBridgeCallbacks_H__

#include <jni.h>
#include "AccessBridgePackages.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef void (*AccessBridge_PropertyChangeFP) (long vmID, JOBJECT64 event, JOBJECT64 source,
                                               wchar_t *property, wchar_t *oldValue, wchar_t *newValue);

typedef void (*AccessBridge_JavaShutdownFP) (long vmID);
typedef void (*AccessBridge_JavaShutdownFP) (long vmID);

typedef void (*AccessBridge_FocusGainedFP) (long vmID, JOBJECT64 event, JOBJECT64 source);
typedef void (*AccessBridge_FocusLostFP) (long vmID, JOBJECT64 event, JOBJECT64 source);

typedef void (*AccessBridge_CaretUpdateFP) (long vmID, JOBJECT64 event, JOBJECT64 source);

typedef void (*AccessBridge_MouseClickedFP) (long vmID, JOBJECT64 event, JOBJECT64 source);
typedef void (*AccessBridge_MouseEnteredFP) (long vmID, JOBJECT64 event, JOBJECT64 source);
typedef void (*AccessBridge_MouseExitedFP) (long vmID, JOBJECT64 event, JOBJECT64 source);
typedef void (*AccessBridge_MousePressedFP) (long vmID, JOBJECT64 event, JOBJECT64 source);
typedef void (*AccessBridge_MouseReleasedFP) (long vmID, JOBJECT64 event, JOBJECT64 source);

typedef void (*AccessBridge_MenuCanceledFP) (long vmID, JOBJECT64 event, JOBJECT64 source);
typedef void (*AccessBridge_MenuDeselectedFP) (long vmID, JOBJECT64 event, JOBJECT64 source);
typedef void (*AccessBridge_MenuSelectedFP) (long vmID, JOBJECT64 event, JOBJECT64 source);
typedef void (*AccessBridge_PopupMenuCanceledFP) (long vmID, JOBJECT64 event, JOBJECT64 source);
typedef void (*AccessBridge_PopupMenuWillBecomeInvisibleFP) (long vmID, JOBJECT64 event, JOBJECT64 source);
typedef void (*AccessBridge_PopupMenuWillBecomeVisibleFP) (long vmID, JOBJECT64 event, JOBJECT64 source);

typedef void (*AccessBridge_PropertyNameChangeFP) (long vmID, JOBJECT64 event, JOBJECT64 source,
                                                                                               wchar_t *oldName, wchar_t *newName);
typedef void (*AccessBridge_PropertyDescriptionChangeFP) (long vmID, JOBJECT64 event, JOBJECT64 source,
                                                                                                      wchar_t *oldDescription, wchar_t *newDescription);
typedef void (*AccessBridge_PropertyStateChangeFP) (long vmID, JOBJECT64 event, JOBJECT64 source,
                                                                                                wchar_t *oldState, wchar_t *newState);
typedef void (*AccessBridge_PropertyValueChangeFP) (long vmID, JOBJECT64 event, JOBJECT64 source,
                                                                                                wchar_t *oldValue, wchar_t *newValue);
typedef void (*AccessBridge_PropertySelectionChangeFP) (long vmID, JOBJECT64 event, JOBJECT64 source);
typedef void (*AccessBridge_PropertyTextChangeFP) (long vmID, JOBJECT64 event, JOBJECT64 source);
typedef void (*AccessBridge_PropertyCaretChangeFP) (long vmID, JOBJECT64 event, JOBJECT64 source,
                                                                                                int oldPosition, int newPosition);
typedef void (*AccessBridge_PropertyVisibleDataChangeFP)  (long vmID, JOBJECT64 event, JOBJECT64 source);
typedef void (*AccessBridge_PropertyChildChangeFP) (long vmID, JOBJECT64 event, JOBJECT64 source,
                                                                                                JOBJECT64 oldChild, JOBJECT64 newChild);
typedef void (*AccessBridge_PropertyActiveDescendentChangeFP) (long vmID, JOBJECT64 event,
                                                               JOBJECT64 source,
                                                               JOBJECT64 oldActiveDescendent,
                                                               JOBJECT64 newActiveDescendent);

typedef void (*AccessBridge_PropertyTableModelChangeFP) (long vmID, JOBJECT64 event, JOBJECT64 src,
                                                                                                             wchar_t *oldValue, wchar_t *newValue);

#ifdef __cplusplus
}
#endif

#endif
