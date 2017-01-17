package main

import (
	"fmt"
	"log"
	"m3u8"
)

func main() {

	opt := m3u8.NewOptions()
	opt.TempDir = "/home/yourok/tmp/"
	//	opt.Url = "file:///home/yourok/test.m3u"
	//	opt.Url = "http://api.new.livestream.com/accounts/22931184/events/6846118/videos/146115126.m3u8"
	opt.Url = "http://atv-vod.ercdn.net/eskiya_dunyaya_hukumdar_olmaz/050/eskiya_dunyaya_hukumdar_olmaz_050.smil/chunklist_b1200000_t64MTIwMGticHM=.m3u8?st=UMUgMJhxlvjQEdm1fxfT_Q&e=1484251384&SessionID=1qhyziauy3dwxx4yxfydiu2m&StreamGroup=eskiya-dunyaya-hukumdar-olmaz&Site=atv&DeviceGroup=web"
	opt.Name = "test"
	opt.OutFileDir = opt.TempDir
	opt.Threads = 10
	opt.Timeout = 60000

	m := m3u8.NewM3U8(opt)
	go func() {
		var err error
		if m.GetList() == nil || m.GetList().Url != opt.Url {
			err = m.LoadList()
		}
		if err == nil {
			/*for i := 1; i < m.GetList().ItemsSize(); i++ {
				m.GetList().GetItem(i).IsLoad = false
			}*/
			for true {
				fmt.Println("Load")
				err = m.Load()
				if err == nil {
					break
				}
				if err != nil {
					fmt.Println(err)
				}
			}
			if err == nil {
				fmt.Println("Join")
				err = m.Join()
				if err == nil {
					fmt.Println("Remove temp")
					//					err = m.RemoveTemp()
					if err == nil {
						m.Finish()
					}
				}
			}
		}
		if err != nil {
			fmt.Println("Error work", err)
		}
	}()
	//	stoped := 0
	log.Println("receive state")
	sg := m3u8.Stage_LoadingList
	for true {
		st := m3u8.GetState(m)
		if st == nil {
			continue
		}
		sg = st.Stage
		log.Println(st, m.Speed())
		if sg == m3u8.Stage_Finished {
			break
		}
	}
	fmt.Println("Exit")
}
