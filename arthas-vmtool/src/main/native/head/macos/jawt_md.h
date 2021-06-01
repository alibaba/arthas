/*
 * Copyright (c) 1999, 2013, Oracle and/or its affiliates. All rights reserved.
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

#ifndef _JAVASOFT_JAWT_MD_H_
#define _JAVASOFT_JAWT_MD_H_

#include "jawt.h"

#ifdef __OBJC__
#import <QuartzCore/CALayer.h>
#endif

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Mac OS X specific declarations for AWT native interface.
 * See notes in jawt.h for an example of use.
 */

/*
 * When calling JAWT_GetAWT with a JAWT version less than 1.7, you must pass this
 * flag or you will not be able to get a valid drawing surface and JAWT_GetAWT will
 * return false. This is to maintain compatibility with applications that used the
 * interface with Java 6 which had multiple rendering models. This flag is not necessary
 * when JAWT version 1.7 or greater is used as this is the only supported rendering mode.
 *
 * Example:
 *   JAWT awt;
 *   awt.version = JAWT_VERSION_1_4 | JAWT_MACOSX_USE_CALAYER;
 *   jboolean success = JAWT_GetAWT(env, &awt);
 */
#define JAWT_MACOSX_USE_CALAYER 0x80000000

/*
 * When the native Cocoa toolkit is in use, the pointer stored in
 * JAWT_DrawingSurfaceInfo->platformInfo points to a NSObject that conforms to the
 * JAWT_SurfaceLayers protocol. Setting the layer property of this object will cause the
 * specified layer to be overlaid on the Components rectangle. If the window the
 * Component belongs to has a CALayer attached to it, this layer will be accessible via
 * the windowLayer property.
 */
#ifdef __OBJC__
@protocol JAWT_SurfaceLayers
@property (readwrite, retain) CALayer *layer;
@property (readonly) CALayer *windowLayer;
@end
#endif

#ifdef __cplusplus
}
#endif

#endif /* !_JAVASOFT_JAWT_MD_H_ */
