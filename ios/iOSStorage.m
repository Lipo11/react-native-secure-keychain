#import "iOSStorage.h"

@implementation iOSStorage

+ (NSString *)applicationDocumentsDirectory
{
    return [[[NSFileManager defaultManager] URLsForDirectory:NSDocumentDirectory inDomains:NSUserDomainMask] lastObject].path;
}

+ (NSString *)getFullPath:(NSString *)path
{
    return [NSString stringWithFormat:@"%@/Secure/%@", [iOSStorage applicationDocumentsDirectory], path];
}

+ (BOOL)exists:(NSString *)path
{
    return [[NSFileManager defaultManager] fileExistsAtPath:[iOSStorage getFullPath:path]];
}

+ (BOOL)write:(NSString *)data to:(NSString *)path
{
    NSFileManager * NSFM = [NSFileManager defaultManager];
    NSString * fullPath = [iOSStorage getFullPath:path];
    NSString * fullDirectory = [fullPath stringByDeletingLastPathComponent];
    
    if( ![NSFM fileExistsAtPath:fullDirectory] ){ [NSFM createDirectoryAtPath:fullDirectory withIntermediateDirectories:YES attributes:nil error:nil]; }
    
    return  [NSFM createFileAtPath:fullPath contents:nil attributes:nil] && [data writeToFile:fullPath atomically:YES encoding:NSUTF8StringEncoding error:nil];
}

+ (NSString *)read:(NSString *)path
{
    NSString * fullPath = [iOSStorage getFullPath:path];
    NSString * data = [NSString stringWithContentsOfFile:fullPath usedEncoding:nil error:nil];
    
    return ( data != nil ) ? data : @"";
}

+ (BOOL)remove:(NSString *)path
{
    NSString * fullPath = [iOSStorage getFullPath:path];
    
    return [[NSFileManager defaultManager] removeItemAtPath:fullPath error:nil];
}

+ (BOOL)removeAll
{
    return [[NSFileManager defaultManager] removeItemAtPath:[NSString stringWithFormat:@"%@/Secure/", [iOSStorage applicationDocumentsDirectory]] error:nil];
}

@end
